package com.example.Apex.strategy.impl;

import com.example.Apex.market.MarketTick;
import com.example.Apex.strategy.StrategySignal;
import com.example.Apex.strategy.TradingStrategy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component("SMA")
public class SimpleMovingAverageStrategy implements TradingStrategy {

    private static final int PERIOD = 20;

    @Override
    public String getStrategyName() {
        return "Simple Moving Average (SMA)";
    }

    @Override
    public StrategySignal analyze(MarketTick currentTick, List<MarketTick> history) {
        if (history.size() < PERIOD) {
            return new StrategySignal(getStrategyName(), StrategySignal.SignalType.HOLD, 0.0,
                    "Insufficient data history");
        }

        // Calculate Average of last 20 ticks
        double sum = history.stream()
                .skip(Math.max(0, history.size() - PERIOD))
                .map(tick -> tick.getPrice().doubleValue())
                .reduce(0.0, Double::sum);

        double average = sum / Math.min(history.size(), PERIOD);
        double currentPrice = currentTick.getPrice().doubleValue();

        if (currentPrice > average) {
            return new StrategySignal(getStrategyName(), StrategySignal.SignalType.BUY, 0.8,
                    String.format("Price %.2f > SMA %.2f", currentPrice, average));
        } else if (currentPrice < average) {
            return new StrategySignal(getStrategyName(), StrategySignal.SignalType.SELL, 0.8,
                    String.format("Price %.2f < SMA %.2f", currentPrice, average));
        } else {
            return new StrategySignal(getStrategyName(), StrategySignal.SignalType.HOLD, 0.5, "Price equals SMA");
        }
    }
}
