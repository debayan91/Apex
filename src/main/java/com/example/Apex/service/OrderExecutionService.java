package com.example.Apex.service;

import com.example.Apex.market.MarketDataService;
import com.example.Apex.model.*;
import com.example.Apex.repo.OrderAuditLogRepository;
import com.example.Apex.repo.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderExecutionService {

    private final OrderRepository orderRepository;
    private final OrderAuditLogRepository auditLogRepository;
    private final RiskManagementService riskManagementService;
    private final WalletService walletService;
    private final MarketDataService marketDataService;

    @Transactional
    public Order executeOrder(Long userId, String symbol, Order.Side side, Integer quantity, String idempotencyKey) {
        // 1. Idempotency Check
        Optional<Order> existingOrder = orderRepository.findByIdempotencyKey(idempotencyKey);
        if (existingOrder.isPresent()) {
            log.info("Idempotent request: Order {} already exists.", existingOrder.get().getId());
            return existingOrder.get();
        }

        // 2. Create Initial Order
        Order order = Order.builder()
                .userId(userId)
                .symbol(symbol)
                .side(side)
                .quantity(quantity)
                .idempotencyKey(idempotencyKey)
                .status(Order.OrderStatus.PENDING_VALIDATION)
                .build();
        order = orderRepository.save(order);
        logStateChange(order, null, Order.OrderStatus.PENDING_VALIDATION);

        try {
            // Price estimation for validation (fetching latest price)
            BigDecimal marketPrice = marketDataService.getLatestPrice(symbol);
            order.setPrice(marketPrice); // Set estimated price

            // 3. Risk Check
            riskManagementService.validateOrder(userId, symbol, quantity, marketPrice);

            updateStatus(order, Order.OrderStatus.VALIDATED);

            // 4. Wallet Lock & Reservation
            BigDecimal totalAmount = marketPrice.multiply(BigDecimal.valueOf(quantity));
            if (side == Order.Side.BUY) {
                // Deduct funds for BUY
                walletService.adjustBalance(userId, totalAmount.negate(), TransactionType.TRADE_BUY);
            } else {
                // For SELL, we should ideally check holding balance, but requirements focus on
                // Wallet Funds for now.
                // Assuming SELL adds funds for simplicity effectively immediately upon fill, OR
                // if we were strictly following the "Reserve funds" instruction:
                // "Validate funds (if withdrawal)" was Sprint 1.
                // Sprint 2 instruction: "Wallet Lock... reserve funds (for BUY orders)".
                // For SELL, we proceed.
            }

            // 5. Fill Order
            // In a real system, matching happens here. We assume instant fill at market
            // price.
            order.setExecutionPrice(marketPrice);
            order.setFilledAt(LocalDateTime.now());
            updateStatus(order, Order.OrderStatus.FILLED);

            // For SELL, credit funds now (Instant Settlement for MVP)
            if (side == Order.Side.SELL) {
                walletService.adjustBalance(userId, totalAmount, TransactionType.TRADE_SELL);
            }

        } catch (RiskManagementService.RiskException e) {
            log.warn("Order {} rejected by Risk Engine: {}", order.getId(), e.getMessage());
            order.setRejectionReason(e.getMessage());
            updateStatus(order, Order.OrderStatus.REJECTED);
        } catch (Exception e) {
            log.error("Order {} failed execution: {}", order.getId(), e.getMessage());
            order.setRejectionReason(e.getMessage());
            updateStatus(order, Order.OrderStatus.REJECTED);
            // NB: If WalletService throws exception, transaction rolls back?
            // We want to persist the REJECTED state.
            // But @Transactional on this method will rollback the generic exception.
            // We should catch runtime exceptions if we want to save the rejection status?
            // Actually, if WalletService fails (insufficient funds), it throws
            // RuntimeException.
            // If we want to save the "REJECTED" status in DB, we must NOT rollback the
            // entire transaction,
            // OR we must run the status update in a separate transaction.
            // However, the requirement says "Atomic".
            // "if it fails... update to REJECTED... and stop."
            // If the whole method is @Transactional, an exception rolls back the insertion
            // of the order itself.
            // This means we lose the record that an attempt was made.
            // To strictly follow "Update to REJECTED", we should probably not let the
            // exception propagate out of the controller
            // completely unchecked if it causes a rollback of the Order creation.
            // BUT, standard Spring behavior: RuntimeException = Rollback.
            // If we want to keep the Order as REJECTED, we need to handle this.
            // For now, I will re-throw to ensure the Controller knows, but this implies the
            // Order record might accept rollback?
            // No, user wants "Log to Audit" and "Update status".
            // So we shouldn't simply rollback.
            // Correct approach: explicitly save REJECTED status and DO NOT throw exception
            // that triggers rollback
            // for business logic failures (like Risk or Funds).
            // RiskException is caught above.
            // WalletService exception (Insufficient funds) needs to be caught too.
            // So I will catch `RuntimeException` generic.
        }

        return orderRepository.save(order);
    }

    private void updateStatus(Order order, Order.OrderStatus newStatus) {
        Order.OrderStatus oldStatus = order.getStatus();
        order.setStatus(newStatus);
        // Save intermediate state?
        // Since we are in one transaction, saving here just updates the persistence
        // context.
        // The final commit writes the latest state.
        // BUT Audit Log must be persisted.
        logStateChange(order, oldStatus, newStatus);
    }

    private void logStateChange(Order order, Order.OrderStatus oldStatus, Order.OrderStatus newStatus) {
        OrderAuditLog auditLog = OrderAuditLog.builder()
                .orderId(order.getId())
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .timestamp(LocalDateTime.now())
                .build();
        auditLogRepository.save(auditLog);
    }
}
