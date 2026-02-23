package com.example.Apex.service;

import com.example.Apex.execution.OrderStateMachine;
import com.example.Apex.market.MarketDataService;
import com.example.Apex.model.*;
import com.example.Apex.repo.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Orchestrates order execution.
 *
 * All order status transitions are delegated entirely to
 * {@link OrderStateMachine}.
 * This class contains zero direct order.setStatus() calls.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OrderExecutionService {

    private final OrderRepository orderRepository;
    private final RiskManagementService riskManagementService;
    private final WalletService walletService;
    private final MarketDataService marketDataService;
    private final OrderStateMachine orderStateMachine;

    @Transactional
    public Order executeOrder(Long userId, String symbol, Order.Side side, Integer quantity, String idempotencyKey) {

        // 1. Idempotency Check
        Optional<Order> existingOrder = orderRepository.findByIdempotencyKey(idempotencyKey);
        if (existingOrder.isPresent()) {
            log.info("Idempotent request: Order {} already exists.", existingOrder.get().getId());
            return existingOrder.get();
        }

        // 2. Create initial order in PENDING_VALIDATION state
        Order order = Order.builder()
                .userId(userId)
                .symbol(symbol)
                .side(side)
                .quantity(quantity)
                .idempotencyKey(idempotencyKey)
                .status(Order.OrderStatus.PENDING_VALIDATION)
                .build();
        order = orderRepository.save(order);

        try {
            // 3. Fetch live market price
            BigDecimal marketPrice = marketDataService.getLatestPrice(symbol);
            order.setPrice(marketPrice);

            // 4. Risk check — throws RiskException on failure
            riskManagementService.validateOrder(userId, symbol, quantity, marketPrice);

            // 5. PENDING_VALIDATION → VALIDATED (enforced by state machine)
            orderStateMachine.transition(order, Order.OrderStatus.VALIDATED);

            // 6. Reserve funds for BUY orders
            BigDecimal totalAmount = marketPrice.multiply(BigDecimal.valueOf(quantity));
            if (side == Order.Side.BUY) {
                walletService.adjustBalance(userId, totalAmount.negate(), TransactionType.TRADE_BUY);
            }

            // 7. VALIDATED → FILLED (enforced by state machine)
            order.setExecutionPrice(marketPrice);
            order.setFilledAt(LocalDateTime.now());
            orderStateMachine.transition(order, Order.OrderStatus.FILLED);

            // 8. Credit funds for SELL orders (instant settlement for MVP)
            if (side == Order.Side.SELL) {
                walletService.adjustBalance(userId, totalAmount, TransactionType.TRADE_SELL);
            }

        } catch (RiskManagementService.RiskException e) {
            log.warn("Order {} rejected by Risk Engine: {}", order.getId(), e.getMessage());
            order.setRejectionReason(e.getMessage());
            // PENDING_VALIDATION → REJECTED (enforced by state machine)
            orderStateMachine.transition(order, Order.OrderStatus.REJECTED);

        } catch (Exception e) {
            log.error("Order {} failed during execution: {}", order.getId(), e.getMessage());
            order.setRejectionReason(e.getMessage());
            // Transition from whatever current state to REJECTED — state machine validates
            // this
            orderStateMachine.transition(order, Order.OrderStatus.REJECTED);
        }

        return orderRepository.save(order);
    }
}
