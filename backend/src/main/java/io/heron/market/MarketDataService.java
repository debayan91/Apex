package io.heron.market;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class MarketDataService {

    private final LatestPriceRepository latestPriceRepository;

    public BigDecimal getLatestPrice(String symbol) {
        return latestPriceRepository.findById(symbol)
                .map(LatestPrice::getPrice)
                .orElseThrow(() -> new RuntimeException("No market data available for symbol: " + symbol));
    }
}

