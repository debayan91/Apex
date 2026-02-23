package com.example.Apex.market;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * DTO for deserializing Binance WebSocket trade stream events.
 * Binance stream format: wss://stream.binance.com:9443/ws/btcusdt@trade
 *
 * Sample payload:
 * {
 * "e": "trade",
 * "E": 1672515782136, <- Event time (epoch ms)
 * "s": "BTCUSDT", <- Symbol
 * "p": "67234.50000000" <- Price
 * }
 */
@Data
public class BinanceTradeEvent {

    @JsonProperty("e")
    private String eventType;

    @JsonProperty("E")
    private Long eventTime;

    @JsonProperty("s")
    private String symbol;

    @JsonProperty("p")
    private BigDecimal price;
}
