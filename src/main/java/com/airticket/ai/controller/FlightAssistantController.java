package com.airticket.ai.controller;

import com.airticket.ai.dto.FlightAssistantChatRequest;
import com.airticket.ai.service.FlightAssistantChatService;
import com.airticket.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/ai/flight-assistant")
public class FlightAssistantController {

    private final FlightAssistantChatService chatService;

    public FlightAssistantController(FlightAssistantChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * SSE 流式聊天接口。
     * 客户端可以按 event 名称分别处理：
     * - session: 当前会话 ID
     * - tool: 即将调用的工具名
     * - delta: 模型逐字输出
     * - usage: token 使用情况
     * - done: 流结束
     * - error: 异常信息
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(@Valid @RequestBody FlightAssistantChatRequest request) {
        return chatService.streamChat(request);
    }

    /**
     * 主动清理某个会话的聊天记忆，便于用户重新开始一轮对话。
     */
    @DeleteMapping("/sessions/{sessionId}")
    public ApiResponse<Boolean> clearSession(@PathVariable String sessionId) {
        return ApiResponse.success("会话记忆已清理", chatService.evictSession(sessionId));
    }
}
