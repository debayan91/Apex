package io.heron.execution;

import io.heron.client.BrokerClient;
import io.heron.exception.OrderExecutionException;
import io.heron.model.Order;
import io.heron.model.TradeLog;
import io.heron.repo.OrderRepository;
import io.heron.repo.TradeLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service responsible for executing orders via the broker.
 * Handles order state management and execution logging.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExecutionService {

    private final BrokerClient brokerClient;
    private final OrderRepository orderRepository;
    private final TradeLogRepository tradeLogRepository;

    /**
     * Execute an order through the broker and update its status.
     * 
     * @param order the order to execute
     * @return the executed order
     * @throws OrderExecutionException if execution fails
     */
    @Transactional
    public Order executeOrder(Order order) {
        log.info("Executing order: {}", order.getId());

        try {
            // Call broker to execute
            String executionId = brokerClient.executeOrder(order);

            // Update order status
            order.setStatus(Order.OrderStatus.FILLED);
            order.setFilledAt(LocalDateTime.now());
            Order savedOrder = orderRepository.save(order);

            // Log execution
            logTrade(savedOrder.getId(), "ORDER_FILLED",
                    String.format("Executed via broker. ExecutionId: %s", executionId));

            log.info("Order {} executed successfully", savedOrder.getId());
            return savedOrder;

        } catch (Exception e) {
            // Mark order as failed
            order.setStatus(Order.OrderStatus.REJECTED);
            orderRepository.save(order);

            logTrade(order.getId(), "ORDER_FAILED", e.getMessage());
            log.error("Order execution failed: {}", e.getMessage(), e);
            throw new OrderExecutionException("Failed to execute order: " + e.getMessage(), e);
        }
    }

    private void logTrade(Long orderId, String action, String details) {
        TradeLog log = TradeLog.builder()
                .orderId(orderId)
                .action(action)
                .details(details)
                .build();
        tradeLogRepository.save(log);
    }
}
