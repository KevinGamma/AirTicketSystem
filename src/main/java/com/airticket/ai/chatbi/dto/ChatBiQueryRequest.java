package com.airticket.ai.chatbi.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatBiQueryRequest(
    @NotBlank(message = "分析问题不能为空")
    String question
) {
}
