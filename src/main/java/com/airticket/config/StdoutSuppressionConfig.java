package com.airticket.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

@Configuration
public class StdoutSuppressionConfig {

    @Value("${app.logging.suppress-stdout:true}")
    private boolean suppressStdout;

    @PostConstruct
    public void suppressStdoutIfConfigured() {
        if (suppressStdout) {
            System.setOut(new PrintStream(OutputStream.nullOutputStream(), true, StandardCharsets.UTF_8));
        }
    }
}
