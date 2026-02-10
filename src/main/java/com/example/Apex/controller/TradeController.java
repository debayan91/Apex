package com.example.Apex.controller;

import com.example.Apex.model.Order;
import com.example.Apex.model.dto.TradeRequest;
import com.example.Apex.model.dto.TradeResponse;
import com.example.Apex.repo.OrderRepository;
import com.example.Apex.service.TradeOrchestrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for trade operations.
 * Provides endpoints to execute trades and view order history.
 */
@Slf4j
@RestController
@RequestMapping("/trade")
@RequiredArgsConstructor
public class TradeController {

    private final TradeOrchestrationService tradeOrchestrationService;
    private final OrderRepository orderRepository;

    @GetMapping("/ping")
    public String ping() {
        return "APEX trade engine ready";
    }

    /**
     * Execute a new trade.
     * POST /trade/execute
     */
    @PostMapping("/execute")
    public ResponseEntity<TradeResponse> executeTrade(@Valid @RequestBody TradeRequest request) {
        log.info("Received trade request: {}", request);
        TradeResponse response = tradeOrchestrationService.executeTrade(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Get order history for a user.
     * GET /trade/history/{userId}
     */
    @GetMapping("/history/{userId}")
    public ResponseEntity<List<Order>> getOrderHistory(@PathVariable Long userId) {
        log.info("Fetching order history for user {}", userId);
        List<Order> orders = orderRepository.findByUserId(userId);
        return ResponseEntity.ok(orders);
    }
}
