package com.example.Apex.strategy;

import com.example.Apex.client.AIClient;

import java.math.BigDecimal;

/**
 * Interface defining the contract for trading strategies.
 * Implementations decide whether to execute a trade based on market conditions.
 */
public interface TradingStrategy {

    /**
     * Determine if a trade should be executed.
     * 
     * @param symbol    the trading symbol
     * @param price     current market price
     * @param sentiment AI sentiment analysis result
     * @return true if trade should execute, false otherwise
     */
    boolean shouldExecute(String symbol, BigDecimal price, AIClient.Sentiment sentiment);

    /**
     * Get the strategy name for logging purposes.
     */
    String getStrategyName();
}
