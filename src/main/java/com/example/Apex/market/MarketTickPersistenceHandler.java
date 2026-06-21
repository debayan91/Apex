package com.example.Apex.market;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Handles persistence of incoming Binance trade events.
 * Deliberately separated from connection logic so each class has a single
 * responsibility.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class MarketTickPersistenceHandler {

    private final MarketTickRepository marketTickRepository;

    /**
     * Converts a BinanceTradeEvent into a MarketTick entity and saves it.
     *
     * @param event parsed trade event from the WebSocket stream
     */
    public void persist(BinanceTradeEvent event) {
        try {
            LocalDateTime timestamp = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(event.getEventTime()),
                    ZoneOffset.UTC);

            MarketTick tick = new MarketTick(event.getSymbol(), event.getPrice(), timestamp);
            marketTickRepository.save(tick);

            log.debug("Persisted tick: {} @ {} [{}]", event.getSymbol(), event.getPrice(), timestamp);
        } catch (Exception e) {
            log.error("Failed to persist MarketTick for event {}: {}", event, e.getMessage(), e);
        }
    }
}
