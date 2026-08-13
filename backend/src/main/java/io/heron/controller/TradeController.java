package io.heron.controller;

import io.heron.model.Order;
import io.heron.repo.OrderRepository;
import io.heron.service.TradeOrchestrationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
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
        return "HERON trade engine ready";
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

    public record TradeRequest(
            @NotNull(message = "User ID is required") Long userId,
            @NotBlank(message = "Symbol is required") String symbol,
            @NotNull(message = "Quantity is required") @Min(value = 1, message = "Quantity must be at least 1") Integer quantity,
            @NotNull(message = "Side is required") Order.Side side,
            String strategyType) {
    }

    public record TradeResponse(boolean success, Long orderId, String message, BigDecimal executedPrice, String details) {
    }
}
