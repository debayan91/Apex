package com.example.Apex.strategy;

import com.example.Apex.client.AIClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Conservative trading strategy.
 * Only executes when sentiment is POSITIVE and price is below threshold.
 * More risk-averse than SimpleMomentumStrategy.
 */
@Slf4j
@Component("CONSERVATIVE")
public class ConservativeStrategy implements TradingStrategy {

    private static final BigDecimal MAX_PRICE_THRESHOLD = new BigDecimal("300.00");

    @Override
    public boolean shouldExecute(String symbol, BigDecimal price, AIClient.Sentiment sentiment) {
        boolean positiveSignal = sentiment == AIClient.Sentiment.POSITIVE;
        boolean reasonablePrice = price.compareTo(MAX_PRICE_THRESHOLD) < 0;
        boolean decision = positiveSignal && reasonablePrice;

        log.info("Strategy {}: symbol={}, price={}, sentiment={}, priceOK={}, decision={}",
                getStrategyName(), symbol, price, sentiment, reasonablePrice, decision ? "EXECUTE" : "SKIP");
        return decision;
    }

    @Override
    public String getStrategyName() {
        return "Conservative";
    }
}
