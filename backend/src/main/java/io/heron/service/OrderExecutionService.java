package io.heron.service;

import io.heron.execution.OrderStateMachine;
import io.heron.market.MarketDataService;
import io.heron.model.*;
import io.heron.repo.OrderRepository;
import io.heron.repo.HoldingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Orchestrates order execution.
 *
 * All order status transitions are delegated entirely to {@link OrderStateMachine}.
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
    private final HoldingRepository holdingRepository;

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

            BigDecimal totalAmount = marketPrice.multiply(BigDecimal.valueOf(quantity));

            // 5. Pre-check funds & holdings
            if (side == Order.Side.BUY) {
                Wallet wallet = walletService.getWallet(userId);
                if (wallet.getBalance().compareTo(totalAmount) < 0) {
                    throw new IllegalArgumentException(String.format("Insufficient funds. Available: $%.2f, Required: $%.2f", wallet.getBalance(), totalAmount));
                }
            } else if (side == Order.Side.SELL) {
                Optional<Holding> holding = holdingRepository.findByUserIdAndSymbol(userId, symbol);
                int heldQty = holding.map(Holding::getQuantity).orElse(0);
                if (heldQty < quantity) {
                    throw new IllegalArgumentException(String.format("Insufficient holdings for %s. Held: %d, Requested: %d", symbol, heldQty, quantity));
                }
            }

            // 6. PENDING_VALIDATION → VALIDATED
            orderStateMachine.transition(order, Order.OrderStatus.VALIDATED);

            // 7. Execute settlement and update holdings
            if (side == Order.Side.BUY) {
                walletService.adjustBalance(userId, totalAmount.negate(), TransactionType.TRADE_BUY);
                updateBuyHolding(userId, symbol, quantity, marketPrice);
            } else {
                walletService.adjustBalance(userId, totalAmount, TransactionType.TRADE_SELL);
                updateSellHolding(userId, symbol, quantity);
            }

            // 8. VALIDATED → FILLED
            order.setExecutionPrice(marketPrice);
            order.setFilledAt(LocalDateTime.now());
            orderStateMachine.transition(order, Order.OrderStatus.FILLED);

        } catch (RiskManagementService.RiskException e) {
            log.warn("Order {} rejected by Risk Engine: {}", order.getId(), e.getMessage());
            order.setRejectionReason(e.getMessage());
            orderStateMachine.transition(order, Order.OrderStatus.REJECTED);

        } catch (Exception e) {
            log.error("Order {} rejected during execution: {}", order.getId(), e.getMessage());
            order.setRejectionReason(e.getMessage());
            orderStateMachine.transition(order, Order.OrderStatus.REJECTED);
        }

        return orderRepository.save(order);
    }

    private void updateBuyHolding(Long userId, String symbol, int quantity, BigDecimal price) {
        Optional<Holding> existing = holdingRepository.findByUserIdAndSymbol(userId, symbol);
        if (existing.isPresent()) {
            Holding h = existing.get();
            BigDecimal totalCost = h.getAveragePrice().multiply(BigDecimal.valueOf(h.getQuantity()))
                    .add(price.multiply(BigDecimal.valueOf(quantity)));
            int newQty = h.getQuantity() + quantity;
            BigDecimal newAvg = totalCost.divide(BigDecimal.valueOf(newQty), 2, java.math.RoundingMode.HALF_UP);
            h.setQuantity(newQty);
            h.setAveragePrice(newAvg);
            holdingRepository.save(h);
        } else {
            Holding h = Holding.builder()
                    .userId(userId)
                    .symbol(symbol)
                    .quantity(quantity)
                    .averagePrice(price)
                    .build();
            holdingRepository.save(h);
        }
    }

    private void updateSellHolding(Long userId, String symbol, int quantity) {
        Holding h = holdingRepository.findByUserIdAndSymbol(userId, symbol)
                .orElseThrow(() -> new IllegalArgumentException("No holding found for symbol: " + symbol));
        int newQty = h.getQuantity() - quantity;
        if (newQty <= 0) {
            holdingRepository.delete(h);
        } else {
            h.setQuantity(newQty);
            holdingRepository.save(h);
        }
    }
}
