package com.example.Apex.controller;

import com.example.Apex.market.LatestPrice;
import com.example.Apex.market.LatestPriceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/prices")
@RequiredArgsConstructor
public class MarketDataController {

    private final LatestPriceRepository latestPriceRepository;

    @GetMapping("/latest")
    public ResponseEntity<List<LatestPrice>> getLatestPrices() {
        List<LatestPrice> latestPrices = latestPriceRepository.findAll();
        return ResponseEntity.ok(latestPrices);
    }
}
