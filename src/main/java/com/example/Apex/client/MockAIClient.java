package com.example.Apex.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Random;

/**
 * Mock implementation of AIClient.
 * Returns random sentiment values.
 * Replace with real AI/ML service integration in production.
 */
@Slf4j
@Component
public class MockAIClient implements AIClient {

    private final Random random = new Random();

    @Override
    public Sentiment getSentiment(String symbol) {
        Sentiment[] sentiments = Sentiment.values();
        Sentiment result = sentiments[random.nextInt(sentiments.length)];
        log.info("Mock AI: sentiment for {} = {}", symbol, result);
        return result;
    }
}
