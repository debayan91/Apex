package com.example.Apex.service;

import com.example.Apex.client.AIClient;
import com.example.Apex.client.BrokerClient;
import com.example.Apex.execution.ExecutionService;
import com.example.Apex.model.Order;
import com.example.Apex.model.User;
import com.example.Apex.model.dto.TradeRequest;
import com.example.Apex.model.dto.TradeResponse;
import com.example.Apex.portfolio.PortfolioService;
import com.example.Apex.repo.OrderRepository;
import com.example.Apex.risk.RiskGuard;
import com.example.Apex.strategy.TradingStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Main orchestration service for the trading flow.
 * Coordinates: price fetch → AI sentiment → strategy decision → risk validation
 * → execution → portfolio update.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TradeOrchestrationService {

        private final BrokerClient brokerClient;
        private final AIClient aiClient;
        private final UserService userService;
        private final RiskGuard riskGuard;
        private final ExecutionService executionService;
        private final PortfolioService portfolioService;
        private final OrderRepository orderRepository;
        private final Map<String, TradingStrategy> strategies;

        /**
         * Execute the complete trading workflow.
         * Returns a TradeResponse indicating success or failure.
         */
        @Transactional
        public TradeResponse executeTrade(TradeRequest request) {
                log.info("=== Starting trade execution for user {} ===", request.getUserId());

                try {
                        // 1. Fetch user
                        User user = userService.findById(request.getUserId());

                        // 2. Fetch current price
                        BigDecimal price = brokerClient.getPrice(request.getSymbol());
                        log.info("Fetched price for {}: {}", request.getSymbol(), price);

                        // 3. Get AI sentiment (optional for strategies)
                        AIClient.Sentiment sentiment = aiClient.getSentiment(request.getSymbol());
                        log.info("AI sentiment for {}: {}", request.getSymbol(), sentiment);

                        // 4. Apply trading strategy
                        String strategyName = request.getStrategyType() != null
                                        ? request.getStrategyType()
                                        : "SIMPLE_MOMENTUM";
                        TradingStrategy strategy = strategies.get(strategyName);
                        if (strategy == null) {
                                return TradeResponse.builder()
                                                .success(false)
                                                .message("Unknown strategy: " + strategyName)
                                                .build();
                        }

                        boolean shouldExecute = strategy.shouldExecute(request.getSymbol(), price, sentiment);
                        if (!shouldExecute) {
                                log.info("Strategy decided NOT to execute trade");
                                return TradeResponse.builder()
                                                .success(false)
                                                .message("Strategy decision: SKIP")
                                                .details(String.format(
                                                                "Strategy %s decided not to execute based on price=%s and sentiment=%s",
                                                                strategy.getStrategyName(), price, sentiment))
                                                .build();
                        }

                        // 5. Risk validation
                        BigDecimal tradeValue = price.multiply(BigDecimal.valueOf(request.getQuantity()));
                        riskGuard.validateTrade(user, tradeValue, request.getQuantity());

                        // 6. Create pending order
                        Order order = Order.builder()
                                        .userId(user.getId())
                                        .symbol(request.getSymbol())
                                        .quantity(request.getQuantity())
                                        .price(price)
                                        .side(request.getSide())
                                        .status(Order.OrderStatus.PENDING_VALIDATION)
                                        .build();
                        Order savedOrder = orderRepository.save(order);

                        // 7. Execute order via broker
                        Order executedOrder = executionService.executeOrder(savedOrder);

                        // 8. Update portfolio
                        portfolioService.updatePortfolio(executedOrder);

                        log.info("=== Trade execution completed successfully for order {} ===", executedOrder.getId());

                        return TradeResponse.builder()
                                        .success(true)
                                        .orderId(executedOrder.getId())
                                        .executedPrice(price)
                                        .message("Trade executed successfully")
                                        .details(String.format("Executed %s %d shares of %s at %s",
                                                        request.getSide(), request.getQuantity(), request.getSymbol(),
                                                        price))
                                        .build();

                } catch (Exception e) {
                        log.error("Trade execution failed: {}", e.getMessage(), e);
                        return TradeResponse.builder()
                                        .success(false)
                                        .message(e.getMessage())
                                        .build();
                }
        }
}
