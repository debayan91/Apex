package com.example.Apex.strategy;

import com.example.Apex.client.AIClient;
import com.example.Apex.market.MarketTick;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Simple momentum strategy based on AI sentiment.
 * Executes BUY orders when sentiment is POSITIVE.
 * Skips NEUTRAL and NEGATIVE sentiments.
 */
@Slf4j
@Component("SIMPLE_MOMENTUM")
@RequiredArgsConstructor
public class SimpleMomentumStrategy implements TradingStrategy {

    private final AIClient aiClient;

    @Override
    public StrategySignal analyze(MarketTick currentTick, List<MarketTick> history) {
        AIClient.Sentiment sentiment = aiClient.getSentiment(currentTick.getSymbol());
        boolean decision = sentiment == AIClient.Sentiment.POSITIVE;

        log.info("Strategy {}: symbol={}, price={}, sentiment={}, decision={}",
                getStrategyName(), currentTick.getSymbol(), currentTick.getPrice(), sentiment,
                decision ? "EXECUTE" : "SKIP");

        if (decision) {
            return new StrategySignal(getStrategyName(), StrategySignal.SignalType.BUY, 0.6, "Sentiment is POSITIVE");
        } else {
            return new StrategySignal(getStrategyName(), StrategySignal.SignalType.HOLD, 0.0,
                    "Sentiment is not POSITIVE");
        }
    }

    @Override
    public String getStrategyName() {
        return "SimpleMomentum";
    }
}
