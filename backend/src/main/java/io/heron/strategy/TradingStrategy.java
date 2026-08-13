package io.heron.strategy;

import io.heron.market.MarketTick;
import java.util.List;

public interface TradingStrategy {
    StrategySignal analyze(MarketTick currentTick, List<MarketTick> history);

    String getStrategyName();
}
