package com.example.Apex.config;

import com.example.Apex.client.AIClient;
import com.example.Apex.client.BrokerClient;
import com.example.Apex.model.Order;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.Random;

/**
 * Configuration for external client beans.
 * Allows easy switching between mock and real implementations.
 */
@Configuration
public class ClientConfig {

    private final Random random = new Random();

    /**
     * Default broker client.
     */
    @Bean
    public BrokerClient brokerClient() {
        return new BrokerClient() {
            @Override
            public BigDecimal getPrice(String symbol) {
                // Generate a random price between 50 and 500
                double price = 50 + (random.nextDouble() * 450);
                return BigDecimal.valueOf(price).setScale(2, java.math.RoundingMode.HALF_UP);
            }

            @Override
            public String executeOrder(Order order) {
                return "MOCK_EXECUTION_ID_" + System.currentTimeMillis();
            }
        };
    }

    /**
     * Default AI client.
     */
    @Bean
    public AIClient aiClient() {
        return symbol -> {
            AIClient.Sentiment[] sentiments = AIClient.Sentiment.values();
            return sentiments[random.nextInt(sentiments.length)];
        };
    }
}
