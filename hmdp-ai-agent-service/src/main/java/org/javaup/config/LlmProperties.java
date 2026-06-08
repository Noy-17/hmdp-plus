package org.javaup.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Data
@Component
@ConfigurationProperties(prefix = "llm")
public class LlmProperties {
    private String baseUrl = "https://api.deepseek.com";
    private String apiKey;
    private String model = "deepseek-chat";
    private int maxTokens = 2000;
    private double temperature = 0.3;
    private Duration timeout = Duration.ofSeconds(30);
}
