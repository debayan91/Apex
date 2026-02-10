package com.example.Apex.strategy;

import com.example.Apex.client.AIClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Simple momentum strategy based on AI sentiment.
 * Executes BUY orders when sentiment is POSITIVE.
 * Skips NEUTRAL and NEGATIVE sentiments.
 */
@Slf4j
@Component("SIMPLE_MOMENTUM")
public class SimpleMomentumStrategy implements TradingStrategy {

    @Override
    public boolean shouldExecute(String symbol, BigDecimal price, AIClient.Sentiment sentiment) {
        boolean decision = sentiment == AIClient.Sentiment.POSITIVE;
        log.info("Strategy {}: symbol={}, price={}, sentiment={}, decision={}",
                getStrategyName(), symbol, price, sentiment, decision ? "EXECUTE" : "SKIP");
        return decision;
    }

    @Override
    public String getStrategyName() {
        return "SimpleMomentum";
    }
}
