package io.heron.strategy.impl;

import io.heron.market.MarketTick;
import io.heron.strategy.StrategySignal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SimpleMovingAverageStrategyTest {

    private SimpleMovingAverageStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new SimpleMovingAverageStrategy();
    }

    @Test
    void testAnalyze_InsufficientHistory_ReturnsHold() {
        List<MarketTick> history = new ArrayList<>();
        MarketTick currentTick = new MarketTick("BTC", new BigDecimal("100"), LocalDateTime.now());
        
        StrategySignal signal = strategy.analyze(currentTick, history);
        
        assertEquals(StrategySignal.SignalType.HOLD, signal.type());
        assertEquals("Insufficient data history", signal.reason());
    }

    @Test
    void testAnalyze_PriceAboveAverage_ReturnsBuy() {
        List<MarketTick> history = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            history.add(new MarketTick("BTC", new BigDecimal("100"), LocalDateTime.now()));
        }
        // Average is 100.
        
        MarketTick currentTick = new MarketTick("BTC", new BigDecimal("105"), LocalDateTime.now());
        
        StrategySignal signal = strategy.analyze(currentTick, history);
        
        assertEquals(StrategySignal.SignalType.BUY, signal.type());
    }

    @Test
    void testAnalyze_PriceBelowAverage_ReturnsSell() {
        List<MarketTick> history = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            history.add(new MarketTick("BTC", new BigDecimal("100"), LocalDateTime.now()));
        }
        // Average is 100.
        
        MarketTick currentTick = new MarketTick("BTC", new BigDecimal("95"), LocalDateTime.now());
        
        StrategySignal signal = strategy.analyze(currentTick, history);
        
        assertEquals(StrategySignal.SignalType.SELL, signal.type());
    }
}
