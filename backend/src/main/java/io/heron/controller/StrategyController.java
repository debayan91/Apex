package io.heron.controller;

import io.heron.market.MarketDataService;
import io.heron.market.MarketTick;
import io.heron.market.MarketTickRepository;
import io.heron.service.BacktestService;
import io.heron.strategy.StrategyRegistry;
import io.heron.strategy.StrategySignal;
import io.heron.strategy.TradingStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
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
                    request.strategyName(),
                    request.start(),
                    request.end());
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    public record BacktestRequest(String strategyName, LocalDateTime start, LocalDateTime end) {
    }
}
