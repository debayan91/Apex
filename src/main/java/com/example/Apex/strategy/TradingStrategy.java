package com.example.Apex.strategy;

import com.example.Apex.market.MarketTick;
import java.util.List;

public interface TradingStrategy {
    StrategySignal analyze(MarketTick currentTick, List<MarketTick> history);

    String getStrategyName();
}
