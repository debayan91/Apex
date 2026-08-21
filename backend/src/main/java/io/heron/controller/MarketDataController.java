package io.heron.controller;

import io.heron.market.LatestPrice;
import io.heron.market.LatestPriceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.bind.annotation.RequestParam;
@RestController
@RequestMapping("/prices")
@RequiredArgsConstructor
public class MarketDataController {

    private final LatestPriceRepository latestPriceRepository;

    @GetMapping("/latest")
    public ResponseEntity<?> getLatestPrices(@RequestParam(required = false) String symbol) {
        if (symbol != null && !symbol.isBlank()) {
            return latestPriceRepository.findById(symbol)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        }
        Iterable<LatestPrice> latestPrices = latestPriceRepository.findAll();
        return ResponseEntity.ok(latestPrices);
    }
}
