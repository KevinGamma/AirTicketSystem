package com.airticket.ai.config;

import com.airticket.ai.agent.FlightAssistantAgent;
import com.airticket.ai.http.SiliconFlowCompatibleHttpClientBuilder;
import com.airticket.ai.memory.RedisChatMemoryStore;
import com.airticket.ai.tool.FlightAssistantTools;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties(FlightAssistantProperties.class)
public class FlightAssistantConfig {

    @Bean
    public OpenAiStreamingChatModel flightAssistantStreamingChatModel(FlightAssistantProperties properties,
                                                                      ObjectMapper objectMapper) {
        return OpenAiStreamingChatModel.builder()
            .httpClientBuilder(new SiliconFlowCompatibleHttpClientBuilder(objectMapper))
            .baseUrl(properties.getBaseUrl())
            .apiKey(properties.getApiKey())
            .modelName(properties.getModelName())
            .temperature(properties.getTemperature())
            .build();
    }

    @Bean
    public RedisChatMemoryStore redisChatMemoryStore(StringRedisTemplate stringRedisTemplate,
                                                     FlightAssistantProperties properties) {
        return new RedisChatMemoryStore(
            stringRedisTemplate,
            Duration.ofSeconds(properties.getMemoryTtlSeconds())
        );
    }

    @Bean
    public ChatMemoryProvider flightAssistantChatMemoryProvider(RedisChatMemoryStore chatMemoryStore,
                                                                FlightAssistantProperties properties) {
        return memoryId -> MessageWindowChatMemory.builder()
            .id(memoryId)
            .maxMessages(properties.getMaxMessages())
            .chatMemoryStore(chatMemoryStore)
            .build();
    }

    @Bean
    public FlightAssistantAgent flightAssistantAgent(OpenAiStreamingChatModel streamingChatModel,
                                                     ChatMemoryProvider flightAssistantChatMemoryProvider,
                                                     FlightAssistantTools flightAssistantTools) {
        return AiServices.builder(FlightAssistantAgent.class)
            .streamingChatModel(streamingChatModel)
            .chatMemoryProvider(flightAssistantChatMemoryProvider)
            .tools(flightAssistantTools)
            .build();
    }
}
