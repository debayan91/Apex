package com.example.Apex.strategy;

import com.example.Apex.client.AIClient;
import com.example.Apex.market.MarketTick;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Conservative trading strategy.
 * Only executes when sentiment is POSITIVE and price is below threshold.
 * More risk-averse than SimpleMomentumStrategy.
 */
@Slf4j
@Component("CONSERVATIVE")
@RequiredArgsConstructor
public class ConservativeStrategy implements TradingStrategy {

    private final AIClient aiClient;
    private static final BigDecimal MAX_PRICE_THRESHOLD = new BigDecimal("300.00");

    @Override
    public StrategySignal analyze(MarketTick currentTick, List<MarketTick> history) {
        AIClient.Sentiment sentiment = aiClient.getSentiment(currentTick.getSymbol());
        BigDecimal price = currentTick.getPrice();

        boolean positiveSignal = sentiment == AIClient.Sentiment.POSITIVE;
        boolean reasonablePrice = price.compareTo(MAX_PRICE_THRESHOLD) < 0;
        boolean decision = positiveSignal && reasonablePrice;

        log.info("Strategy {}: symbol={}, price={}, sentiment={}, priceOK={}, decision={}",
                getStrategyName(), currentTick.getSymbol(), price, sentiment, reasonablePrice,
                decision ? "EXECUTE" : "SKIP");

        if (decision) {
            return new StrategySignal(getStrategyName(), StrategySignal.SignalType.BUY, 0.4,
                    "Conservative criteria met");
        }
        return new StrategySignal(getStrategyName(), StrategySignal.SignalType.HOLD, 0.0, "Criteria not met");
    }

    @Override
    public String getStrategyName() {
        return "Conservative";
    }
}
