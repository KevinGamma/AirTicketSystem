package com.airticket.ai.service;

import com.airticket.ai.agent.FlightAssistantAgent;
import com.airticket.ai.dto.FlightAssistantChatRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.service.TokenStream;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class FlightAssistantChatService {

    private static final DateTimeFormatter SERVER_TIME_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final FlightAssistantAgent flightAssistantAgent;
    private final ObjectMapper objectMapper;
    private final Executor streamingExecutor = Executors.newCachedThreadPool();
    private final Map<String, ReentrantLock> sessionLocks = new ConcurrentHashMap<>();

    public FlightAssistantChatService(FlightAssistantAgent flightAssistantAgent,
                                      ObjectMapper objectMapper) {
        this.flightAssistantAgent = flightAssistantAgent;
        this.objectMapper = objectMapper;
    }

    public SseEmitter streamChat(FlightAssistantChatRequest request) {
        SseEmitter emitter = new SseEmitter(0L);

        CompletableFuture.runAsync(() -> doStream(request, emitter), streamingExecutor)
            .exceptionally(ex -> {
                completeWithError(emitter, ex);
                return null;
            });

        return emitter;
    }

    public boolean evictSession(String sessionId) {
        return flightAssistantAgent.evictChatMemory(sessionId);
    }

    private void doStream(FlightAssistantChatRequest request, SseEmitter emitter) {
        ReentrantLock lock = sessionLocks.computeIfAbsent(request.getSessionId(), ignored -> new ReentrantLock());
        lock.lock();

        try {
            sendEvent(emitter, "session", request.getSessionId());

            ZonedDateTime now = ZonedDateTime.now();
            String currentDateTime = SERVER_TIME_FORMATTER.format(now);
            String currentTimezone = now.getZone().getId();

            CompletableFuture<Void> finished = new CompletableFuture<>();
            TokenStream tokenStream = flightAssistantAgent.chat(
                request.getSessionId(),
                request.getMessage(),
                currentDateTime,
                currentTimezone
            );

            tokenStream
                .onToolExecuted(toolExecution -> {
                    sendEvent(emitter, "tool", toolExecution.request().name());
                    sendFlightSearchResultEventIfPresent(emitter, toolExecution.request().name(), toolExecution.result());
                })
                .onPartialResponse(partial -> sendEvent(emitter, "delta", partial))
                .onCompleteResponse(response -> {
                    TokenUsage tokenUsage = response.tokenUsage();
                    if (tokenUsage != null) {
                        sendEvent(emitter, "usage", tokenUsage.toString());
                    }
                    sendEvent(emitter, "done", "[DONE]");
                    emitter.complete();
                    finished.complete(null);
                })
                .onError(error -> {
                    completeWithError(emitter, error);
                    finished.completeExceptionally(error);
                })
                .start();

            finished.join();
        } finally {
            lock.unlock();
            if (!lock.hasQueuedThreads()) {
                sessionLocks.remove(request.getSessionId(), lock);
            }
        }
    }

    private void sendEvent(SseEmitter emitter, String eventName, String data) {
        try {
            emitter.send(
                SseEmitter.event()
                    .name(eventName)
                    .data(data == null ? "" : data, MediaType.TEXT_PLAIN)
            );
        } catch (IOException ex) {
            throw new RuntimeException("SSE 推送失败", ex);
        }
    }

    private void sendFlightSearchResultEventIfPresent(SseEmitter emitter, String toolName, Object toolResult) {
        if (!"searchFlights".equals(toolName) || toolResult == null) {
            return;
        }

        try {
            String payload = normalizeJsonPayload(toolResult);
            if (payload != null && !payload.isBlank()) {
                sendEvent(emitter, "flight_search_result", payload);
            }
        } catch (Exception ignored) {
            // 结构化辅助事件发送失败时，不影响主对话流
        }
    }

    private String normalizeJsonPayload(Object toolResult) throws Exception {
        if (toolResult instanceof String stringResult) {
            JsonNode node = objectMapper.readTree(stringResult);
            return objectMapper.writeValueAsString(node);
        }
        return objectMapper.writeValueAsString(toolResult);
    }

    private void completeWithError(SseEmitter emitter, Throwable throwable) {
        try {
            sendEvent(emitter, "error", throwable.getMessage());
        } catch (Exception ignored) {
            // 连接已关闭时无需再抛出。
        }
        emitter.completeWithError(throwable);
    }
}
