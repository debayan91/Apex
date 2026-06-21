package com.example.Apex.controller;

import com.example.Apex.market.MarketDataService;
import com.example.Apex.market.MarketTick;
import com.example.Apex.market.MarketTickRepository;
import com.example.Apex.model.dto.BacktestRequest;
import com.example.Apex.service.BacktestService;
import com.example.Apex.strategy.StrategyRegistry;
import com.example.Apex.strategy.StrategySignal;
import com.example.Apex.strategy.TradingStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/strategies")
@RequiredArgsConstructor
public class StrategyController {

    private final StrategyRegistry strategyRegistry;
    private final BacktestService backtestService;
    private final MarketTickRepository marketTickRepository; // Direct access for MVP signal generation
    private final MarketDataService marketDataService;

    @GetMapping
    public Set<String> listStrategies() {
        return strategyRegistry.getAllStrategyNames();
    }

    @PostMapping("/{name}/signal")
    public ResponseEntity<?> getSignal(@PathVariable String name) {
        TradingStrategy strategy = strategyRegistry.getStrategy(name);
        if (strategy == null) {
            return ResponseEntity.notFound().build();
        }

        List<MarketTick> history = marketTickRepository.findAll(); // Simplified for MVP: fetch all.

        if (history.isEmpty()) {
            return ResponseEntity.badRequest().body("No market data available");
        }

        MarketTick currentTick = history.get(history.size() - 1);
        StrategySignal signal = strategy.analyze(currentTick, history);

        return ResponseEntity.ok(signal);
    }

    @PostMapping("/backtest")
    public ResponseEntity<?> runBacktest(@RequestBody BacktestRequest request) {
        try {
            BacktestService.BacktestResult result = backtestService.runBacktest(
                    request.getStrategyName(),
                    request.getStart(),
                    request.getEnd());
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
