package io.heron.controller;

import io.heron.model.Order;
import io.heron.repo.OrderRepository;
import io.heron.service.OrderExecutionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderExecutionService orderExecutionService;
    private final OrderRepository orderRepository;

    @PostMapping
    public ResponseEntity<Order> createOrder(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody OrderRequest request) {

        Order order = orderExecutionService.executeOrder(
                request.userId(),
                request.symbol(),
                request.side(),
                request.quantity(),
                idempotencyKey,
                request.strategyType());

        if (order.getStatus() == Order.OrderStatus.REJECTED) {
            return ResponseEntity.badRequest().body(order);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    @GetMapping("/history/{userId}")
    public ResponseEntity<List<Order>> getOrderHistory(@PathVariable Long userId) {
        List<Order> orders = orderRepository.findByUserId(userId);
        return ResponseEntity.ok(orders);
    }

    public record OrderRequest(
            @NotNull Long userId,
            @NotBlank String symbol,
            @NotNull Order.Side side,
            @NotNull @Min(1) Integer quantity,
            String strategyType) {
    }
}
