package com.example.Apex.service;

import com.example.Apex.market.MarketTick;
import com.example.Apex.market.MarketTickRepository;
import com.example.Apex.strategy.StrategyRegistry;
import com.example.Apex.strategy.StrategySignal;
import com.example.Apex.strategy.TradingStrategy;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class BacktestService {

    private final MarketTickRepository marketTickRepository;
    private final StrategyRegistry strategyRegistry;

    public BacktestResult runBacktest(String strategyName, LocalDateTime start, LocalDateTime end) {
        TradingStrategy strategy = strategyRegistry.getStrategy(strategyName);
        if (strategy == null) {
            throw new IllegalArgumentException("Strategy not found: " + strategyName);
        }

        List<MarketTick> ticks = marketTickRepository.findByTimestampBetweenOrderByTimestampAsc(start, end);
        if (ticks.isEmpty()) {
            return BacktestResult.builder()
                    .strategyName(strategyName)
                    .message("No market data found for the given range")
                    .build();
        }

        log.info("Starting backtest for {} with {} ticks", strategyName, ticks.size());

        List<MarketTick> historyProxy = new ArrayList<>();
        double cash = 10000.0; // Initial simulated capital
        double initialCapital = cash;
        double holdings = 0.0;
        int wins = 0;
        int trades = 0;
        double maxDrawdown = 0.0;
        double peakCapital = initialCapital;

        for (MarketTick tick : ticks) {
            // Update history
            historyProxy.add(tick);

            // Analyze
            StrategySignal signal = strategy.analyze(tick, historyProxy);
            double price = tick.getPrice().doubleValue();

            // Execute (Simulated)
            if (signal.type() == StrategySignal.SignalType.BUY && cash >= price) {
                // Buy 1 unit
                holdings += 1.0;
                cash -= price;
                trades++;
            } else if (signal.type() == StrategySignal.SignalType.SELL && holdings >= 1.0) {
                // Sell 1 unit
                holdings -= 1.0;
                cash += price;
                trades++;
                // Rudimentary win check: if current cash > initial, we are winning?
                // Real win rate needs entry price tracking. ignoring for MVP simplicity.
            }

            // Calculate Portfolio Value
            double portValue = cash + (holdings * price);
            peakCapital = Math.max(peakCapital, portValue);
            double drawdown = (peakCapital - portValue) / peakCapital;
            maxDrawdown = Math.max(maxDrawdown, drawdown);
        }

        double finalValue = cash + (holdings * ticks.get(ticks.size() - 1).getPrice().doubleValue());
        double totalReturn = (finalValue - initialCapital) / initialCapital * 100;

        return BacktestResult.builder()
                .strategyName(strategyName)
                .totalReturnPercent(totalReturn)
                .maxDrawdownPercent(maxDrawdown * 100)
                .totalTrades(trades)
                .message("Backtest completed successfully")
                .build();
    }

    @Data
    @Builder
    public static class BacktestResult {
        private String strategyName;
        private double totalReturnPercent;
        private double maxDrawdownPercent;
        private int totalTrades;
        private String message;
    }
}
