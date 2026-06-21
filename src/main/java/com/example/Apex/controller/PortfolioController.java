package com.example.Apex.controller;

import com.example.Apex.model.dto.PortfolioSummary;
import com.example.Apex.portfolio.PortfolioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for portfolio operations.
 * Provides endpoints to view user portfolios and holdings.
 */
@Slf4j
@RestController
@RequestMapping("/portfolio")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioService portfolioService;

    /**
     * Get complete portfolio summary for a user.
     * GET /portfolio/{userId}
     */
    @GetMapping("/{userId}")
    public ResponseEntity<PortfolioSummary> getPortfolio(@PathVariable Long userId) {
        log.info("Fetching portfolio for user {}", userId);
        PortfolioSummary summary = portfolioService.getPortfolioSummary(userId);
        return ResponseEntity.ok(summary);
    }
}
