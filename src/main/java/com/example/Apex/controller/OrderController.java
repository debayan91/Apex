package com.example.Apex.controller;

import com.example.Apex.model.Order;
import com.example.Apex.service.OrderExecutionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderExecutionService orderExecutionService;

    @PostMapping
    public ResponseEntity<Order> createOrder(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody OrderRequest request) {

        Order order = orderExecutionService.executeOrder(
                request.userId(),
                request.symbol(),
                request.side(),
                request.quantity(),
                idempotencyKey);

        if (order.getStatus() == Order.OrderStatus.REJECTED) {
            return ResponseEntity.badRequest().body(order);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    public record OrderRequest(
            @NotNull Long userId,
            @NotBlank String symbol,
            @NotNull Order.Side side,
            @NotNull @Min(1) Integer quantity) {
    }
}
