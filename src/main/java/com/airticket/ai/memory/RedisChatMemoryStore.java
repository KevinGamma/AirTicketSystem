package com.airticket.ai.memory;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

public class RedisChatMemoryStore implements ChatMemoryStore {

    private static final String KEY_PREFIX = "ai:flight-assistant:memory:";

    private final StringRedisTemplate stringRedisTemplate;
    private final Duration ttl;

    public RedisChatMemoryStore(StringRedisTemplate stringRedisTemplate, Duration ttl) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.ttl = ttl;
    }

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        String payload = stringRedisTemplate.opsForValue().get(buildKey(memoryId));
        if (payload == null || payload.isBlank()) {
            return Collections.emptyList();
        }
        return ChatMessageDeserializer.messagesFromJson(payload);
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        String key = buildKey(memoryId);
        String payload = ChatMessageSerializer.messagesToJson(messages);
        stringRedisTemplate.opsForValue().set(key, payload, ttl);
    }

    @Override
    public void deleteMessages(Object memoryId) {
        stringRedisTemplate.delete(buildKey(memoryId));
    }

    private String buildKey(Object memoryId) {
        return KEY_PREFIX + memoryId;
    }
}
