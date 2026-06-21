package com.example.Apex.strategy;

public record StrategySignal(String strategyName, SignalType type, double confidence, String reason) {
    public enum SignalType {
        BUY, SELL, HOLD
    }
}
