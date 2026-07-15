package com.example.Apex.market;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumer for market ticks from Kafka.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class MarketTickKafkaConsumer {

    private final MarketTickPersistenceHandler persistenceHandler;

    @KafkaListener(topics = "market-ticks", groupId = "apex-market-data-group")
    public void consume(BinanceTradeEvent event) {
        persistenceHandler.persist(event);
    }
}
