package com.example.Apex.market;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Provides query access to the latest market data.
 *
 * Data ingestion is handled entirely by {@link BinanceWebSocketClient}.
 * This service is responsible only for reading persisted ticks.
 */
@Service
@RequiredArgsConstructor
public class MarketDataService {

    private final MarketTickRepository marketTickRepository;

    /**
     * Returns the most recent persisted price for the given symbol.
     *
     * @param symbol e.g. "BTCUSDT"
     * @return latest price
     * @throws RuntimeException if no data has been ingested yet
     */
    public BigDecimal getLatestPrice(String symbol) {
        return marketTickRepository.findTopBySymbolOrderByTimestampDesc(symbol)
                .map(MarketTick::getPrice)
                .orElseThrow(() -> new RuntimeException("No market data available for symbol: " + symbol));
    }
}
