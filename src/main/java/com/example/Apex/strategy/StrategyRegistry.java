package com.example.Apex.strategy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
public class StrategyRegistry {

    private final Map<String, TradingStrategy> strategyMap = new HashMap<>();

    public StrategyRegistry(List<TradingStrategy> strategies) {
        for (TradingStrategy strategy : strategies) {
            strategyMap.put(strategy.getStrategyName(), strategy);
            log.info("Registered strategy: {}", strategy.getStrategyName());
        }
    }

    public Set<String> getAllStrategyNames() {
        return strategyMap.keySet();
    }

    public TradingStrategy getStrategy(String name) {
        return strategyMap.get(name);
    }
}
