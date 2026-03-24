package com.airticket.ai.config;

import com.airticket.ai.chatbi.agent.AnalysisAgent;
import com.airticket.ai.http.SiliconFlowCompatibleHttpClientBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatBiConfig {

    @Bean
    public OpenAiChatModel chatBiChatModel(FlightAssistantProperties properties,
                                           ObjectMapper objectMapper) {
        return OpenAiChatModel.builder()
            .httpClientBuilder(new SiliconFlowCompatibleHttpClientBuilder(objectMapper))
            .baseUrl(properties.getBaseUrl())
            .apiKey(properties.getApiKey())
            .modelName(properties.getModelName())
            .temperature(properties.getTemperature())
            .build();
    }

    @Bean
    public AnalysisAgent analysisAgent(OpenAiChatModel chatBiChatModel) {
        return AiServices.builder(AnalysisAgent.class)
            .chatModel(chatBiChatModel)
            .build();
    }
}
