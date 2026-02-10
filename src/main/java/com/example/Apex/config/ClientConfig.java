package com.example.Apex.config;

import com.example.Apex.client.AIClient;
import com.example.Apex.client.BrokerClient;
import com.example.Apex.client.MockAIClient;
import com.example.Apex.client.MockBrokerClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for external client beans.
 * Allows easy switching between mock and real implementations.
 */
@Configuration
public class ClientConfig {

    /**
     * Default broker client (mock for now).
     * Replace with @Profile("prod") real implementation when available.
     */
    @Bean
    public BrokerClient brokerClient() {
        return new MockBrokerClient();
    }

    /**
     * Default AI client (mock for now).
     * Replace with @Profile("prod") real implementation when available.
     */
    @Bean
    public AIClient aiClient() {
        return new MockAIClient();
    }
}
