package com.ruralhealthcare.ai.config;

import com.ruralhealthcare.ai.client.AiPredictionClient;
import com.ruralhealthcare.ai.client.ExternalAiModelClient;
import com.ruralhealthcare.ai.client.MockAiPredictionClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestClient;

@Configuration
public class AiClientConfig {

    private static final Logger log = LoggerFactory.getLogger(AiClientConfig.class);

    @Bean
    @ConditionalOnMissingBean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    @ConditionalOnProperty(name = "ai.model.provider", havingValue = "external")
    public AiPredictionClient externalAiModelClient(AiConfigProperties properties, RestClient.Builder restClientBuilder) {
        log.info("Configuring EXTERNAL AI Prediction Client pointing to {}", properties.getBaseUrl());
        return new ExternalAiModelClient(properties, restClientBuilder);
    }

    @Bean
    @ConditionalOnMissingBean(AiPredictionClient.class)
    @Primary
    public AiPredictionClient mockAiPredictionClient(AiConfigProperties properties) {
        log.info("Configuring MOCK AI Prediction Client (provider=mock)");
        return new MockAiPredictionClient(properties);
    }
}
