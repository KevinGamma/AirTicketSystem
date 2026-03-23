package com.airticket.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "llm.flight-assistant")
public class FlightAssistantProperties {

    /**
     * 兼容 OpenAI 或兼容 OpenAI API 的网关地址。
     */
    private String baseUrl = "https://api.openai.com/v1";

    /**
     * 模型 API Key，建议通过环境变量注入。
     */
    private String apiKey;

    /**
     * 推荐使用支持工具调用和流式输出的模型。
     */
    private String modelName = "gpt-4o-mini";

    /**
     * 航班推荐场景要求低随机性，避免推荐文案漂移。
     */
    private Double temperature = 0.2D;

    /**
     * 每个会话最多保留多少条消息进入上下文。
     */
    /**
     * SiliconFlow chat/completions 当前要求 messages 数组长度不超过 10。
     * LangChain4j 在一次带工具调用的对话中，还会插入 assistant/tool 消息，
     * 因此这里预留余量，避免多轮对话后触发 20015。
     */
    private Integer maxMessages = 8;

    /**
     * Redis 中聊天记忆的有效期，单位：秒。
     */
    private Long memoryTtlSeconds = 86400L;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public Integer getMaxMessages() {
        return maxMessages;
    }

    public void setMaxMessages(Integer maxMessages) {
        this.maxMessages = maxMessages;
    }

    public Long getMemoryTtlSeconds() {
        return memoryTtlSeconds;
    }

    public void setMemoryTtlSeconds(Long memoryTtlSeconds) {
        this.memoryTtlSeconds = memoryTtlSeconds;
    }
}
