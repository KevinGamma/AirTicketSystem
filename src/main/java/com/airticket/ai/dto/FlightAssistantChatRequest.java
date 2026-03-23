package com.airticket.ai.dto;

import jakarta.validation.constraints.NotBlank;

public class FlightAssistantChatRequest {

    @NotBlank(message = "sessionId 不能为空")
    private String sessionId;

    @NotBlank(message = "message 不能为空")
    private String message;

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
