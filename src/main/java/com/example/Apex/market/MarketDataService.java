package com.example.Apex.market;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class MarketDataService {

    private final MarketTickRepository marketTickRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    private static final String BINANCE_URL = "https://api.binance.com/api/v3/ticker/price?symbol=BTCUSDT";

    @Scheduled(fixedRate = 3000)
    public void fetchBtcPrice() {
        try {
            BinanceTickerDto response = restTemplate.getForObject(BINANCE_URL, BinanceTickerDto.class);
            if (response != null && response.getPrice() != null) {
                MarketTick tick = new MarketTick(response.getSymbol(), response.getPrice(), LocalDateTime.now());
                marketTickRepository.save(tick);
                log.info("Saved tick: {} at {}", tick.getPrice(), tick.getTimestamp());
            } else {
                log.warn("Received empty response from Binance");
            }
        } catch (RestClientException e) {
            log.error("Error fetching price from Binance: {}", e.getMessage());
        }
    }

    public BigDecimal getLatestPrice(String symbol) {
        return marketTickRepository.findTopBySymbolOrderByTimestampDesc(symbol)
                .map(MarketTick::getPrice)
                .orElseThrow(() -> new RuntimeException("No market data available for symbol: " + symbol));
    }

    @Data
    private static class BinanceTickerDto {
        private String symbol;
        private BigDecimal price;
    }
}
