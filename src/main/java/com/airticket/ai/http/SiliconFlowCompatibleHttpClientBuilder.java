package com.airticket.ai.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.langchain4j.exception.HttpException;
import dev.langchain4j.http.client.HttpClient;
import dev.langchain4j.http.client.HttpClientBuilder;
import dev.langchain4j.http.client.HttpRequest;
import dev.langchain4j.http.client.SuccessfulHttpResponse;
import dev.langchain4j.http.client.jdk.JdkHttpClientBuilder;
import dev.langchain4j.http.client.sse.ServerSentEventListener;
import dev.langchain4j.http.client.sse.ServerSentEventParser;

import java.time.Duration;

/**
 * SiliconFlow 当前对 tool calling 的 assistant 消息校验更严格：
 * 当 role=assistant 且带 tool_calls 时，要求显式存在 content 字段，允许为空字符串。
 *
 * LangChain4j 1.0.0 在第二轮工具调用请求中会省略该字段，导致 SiliconFlow 返回：
 * 20015 - "messages" in request are illegal.
 *
 * 这里在 HTTP 发出前做一次最小化补丁，只补 assistant/tool_calls 缺失 content 的场景。
 */
public class SiliconFlowCompatibleHttpClientBuilder implements HttpClientBuilder {

    private final ObjectMapper objectMapper;
    private final JdkHttpClientBuilder delegate = new JdkHttpClientBuilder();

    public SiliconFlowCompatibleHttpClientBuilder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Duration connectTimeout() {
        return delegate.connectTimeout();
    }

    @Override
    public HttpClientBuilder connectTimeout(Duration duration) {
        delegate.connectTimeout(duration);
        return this;
    }

    @Override
    public Duration readTimeout() {
        return delegate.readTimeout();
    }

    @Override
    public HttpClientBuilder readTimeout(Duration duration) {
        delegate.readTimeout(duration);
        return this;
    }

    @Override
    public HttpClient build() {
        HttpClient baseClient = delegate.build();
        return new SiliconFlowCompatibleHttpClient(baseClient, objectMapper);
    }

    private static class SiliconFlowCompatibleHttpClient implements HttpClient {

        private final HttpClient delegate;
        private final ObjectMapper objectMapper;

        private SiliconFlowCompatibleHttpClient(HttpClient delegate, ObjectMapper objectMapper) {
            this.delegate = delegate;
            this.objectMapper = objectMapper;
        }

        @Override
        public SuccessfulHttpResponse execute(HttpRequest request) throws HttpException, RuntimeException {
            return delegate.execute(mutateRequest(request));
        }

        @Override
        public void execute(HttpRequest request,
                            ServerSentEventParser parser,
                            ServerSentEventListener listener) {
            delegate.execute(mutateRequest(request), parser, listener);
        }

        private HttpRequest mutateRequest(HttpRequest request) {
            String body = request.body();
            if (body == null || body.isBlank()) {
                return request;
            }

            try {
                JsonNode root = objectMapper.readTree(body);
                JsonNode messagesNode = root.get("messages");
                if (!(root instanceof ObjectNode rootObject) || !(messagesNode instanceof ArrayNode messages)) {
                    return request;
                }

                boolean changed = false;
                for (JsonNode messageNode : messages) {
                    if (!(messageNode instanceof ObjectNode messageObject)) {
                        continue;
                    }

                    JsonNode roleNode = messageObject.get("role");
                    JsonNode toolCallsNode = messageObject.get("tool_calls");
                    boolean isAssistant = roleNode != null && "assistant".equals(roleNode.asText());
                    boolean hasToolCalls = toolCallsNode != null && toolCallsNode.isArray() && toolCallsNode.size() > 0;
                    boolean missingContent = !messageObject.has("content") || messageObject.get("content").isNull();

                    if (isAssistant && hasToolCalls && missingContent) {
                        messageObject.put("content", "");
                        changed = true;
                    }
                }

                if (!changed) {
                    return request;
                }

                String mutatedBody = objectMapper.writeValueAsString(rootObject);

                HttpRequest.Builder builder = HttpRequest.builder()
                    .method(request.method())
                    .url(request.url())
                    .headers(request.headers())
                    .body(mutatedBody);

                return builder.build();
            } catch (Exception ex) {
                return request;
            }
        }
    }
}
