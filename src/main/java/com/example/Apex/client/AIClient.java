package com.example.Apex.client;

/**
 * Interface for AI sentiment analysis integration.
 */
public interface AIClient {

    /**
     * Get AI-powered sentiment for a symbol.
     * 
     * @param symbol the stock/crypto symbol
     * @return sentiment (POSITIVE, NEGATIVE, NEUTRAL)
     */
    Sentiment getSentiment(String symbol);

    enum Sentiment {
        POSITIVE, NEGATIVE, NEUTRAL
    }
}
