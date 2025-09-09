package com.airticket.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Configuration
public class JacksonConfig {

    private static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern(DATETIME_FORMAT);

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        javaTimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DATETIME_FORMATTER));
        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DATETIME_FORMATTER));

        SimpleModule instantModule = new SimpleModule();

        instantModule.addDeserializer(Instant.class, new com.fasterxml.jackson.databind.JsonDeserializer<Instant>() {
            @Override
            public Instant deserialize(com.fasterxml.jackson.core.JsonParser p, 
                                     com.fasterxml.jackson.databind.DeserializationContext ctxt) 
                    throws java.io.IOException {
                String text = p.getText().trim();
                
                try {
                    return Instant.parse(text);
                } catch (Exception e1) {
                    try {
                        LocalDateTime localDateTime = LocalDateTime.parse(text, DATETIME_FORMATTER);
                        return localDateTime.toInstant(ZoneOffset.UTC);
                    } catch (Exception e2) {
                        try {
                            DateTimeFormatter isoFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
                            LocalDateTime localDateTime = LocalDateTime.parse(text, isoFormatter);
                            return localDateTime.toInstant(ZoneOffset.UTC);
                        } catch (Exception e3) {
                            throw new RuntimeException("Unable to parse date: " + text + ". Supported formats: ISO-8601, yyyy-MM-dd HH:mm:ss, yyyy-MM-ddTHH:mm:ss", e3);
                        }
                    }
                }
            }
        });
        
        return new ObjectMapper()
                .registerModule(javaTimeModule)
                .registerModule(instantModule);
    }
}