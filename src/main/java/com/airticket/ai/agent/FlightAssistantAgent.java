package com.airticket.ai.agent;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.memory.ChatMemoryAccess;

public interface FlightAssistantAgent extends ChatMemoryAccess {

    @SystemMessage(fromResource = "prompts/flight-assistant-system-message.txt")
    TokenStream chat(@MemoryId String sessionId,
                     @UserMessage String userMessage,
                     @V("currentDateTime") String currentDateTime,
                     @V("currentTimezone") String currentTimezone);
}
