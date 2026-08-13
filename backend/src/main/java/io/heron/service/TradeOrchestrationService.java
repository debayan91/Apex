package io.heron.service;

import io.heron.client.BrokerClient;
import io.heron.controller.TradeController;
import io.heron.execution.ExecutionService;
import io.heron.model.Order;
import io.heron.model.User;
import io.heron.portfolio.PortfolioService;
import io.heron.repo.OrderRepository;
import io.heron.risk.RiskGuard;
import io.heron.strategy.TradingStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Main orchestration service for the trading flow.
 * Coordinates: price fetch → strategy decision → risk validation
 * → execution → portfolio update.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TradeOrchestrationService {

        private final BrokerClient brokerClient;
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
        public TradeController.TradeResponse executeTrade(TradeController.TradeRequest request) {
                log.info("=== Starting trade execution for user {} ===", request.userId());

                try {
                        // 1. Fetch user
                        User user = userService.findById(request.userId());

                        // 2. Fetch current price
                        BigDecimal price = brokerClient.getPrice(request.symbol());
                        log.info("Fetched price for {}: {}", request.symbol(), price);

                        // 3. Apply trading strategy
                        String strategyName = request.strategyType() != null
                                        ? request.strategyType()
                                        : "SMA";
                        TradingStrategy strategy = strategies.get(strategyName);
                        if (strategy == null) {
                                return new TradeController.TradeResponse(false, null, "Unknown strategy: " + strategyName, null, null);
                        }

                        // Create a temporary MarketTick for analysis
                        io.heron.market.MarketTick currentTick = new io.heron.market.MarketTick(
                                        request.symbol(), price, java.time.LocalDateTime.now());

                        io.heron.strategy.StrategySignal signal = strategy.analyze(currentTick,
                                        java.util.Collections.emptyList());

                        if (signal.type() != io.heron.strategy.StrategySignal.SignalType.BUY) {
                                log.info("Strategy decided NOT to execute trade");
                                return new TradeController.TradeResponse(false, null, "Strategy decision: " + signal.type(), null, signal.reason());
                        }

                        // 4. Risk validation
                        BigDecimal tradeValue = price.multiply(BigDecimal.valueOf(request.quantity()));
                        riskGuard.validateTrade(user, tradeValue, request.quantity());

                        // 5. Create pending order
                        Order order = Order.builder()
                                        .userId(user.getId())
                                        .symbol(request.symbol())
                                        .quantity(request.quantity())
                                        .price(price)
                                        .side(request.side())
                                        .status(Order.OrderStatus.PENDING_VALIDATION)
                                        .build();
                        Order savedOrder = orderRepository.save(order);

                        // 6. Execute order via broker
                        Order executedOrder = executionService.executeOrder(savedOrder);

                        // 7. Update portfolio
                        portfolioService.updatePortfolio(executedOrder);

                        log.info("=== Trade execution completed successfully for order {} ===", executedOrder.getId());

                        return new TradeController.TradeResponse(
                                        true,
                                        executedOrder.getId(),
                                        "Trade executed successfully",
                                        price,
                                        String.format("Executed %s %d shares of %s at %s",
                                                        request.side(), request.quantity(), request.symbol(), price));

                } catch (Exception e) {
                        log.error("Trade execution failed: {}", e.getMessage(), e);
                        return new TradeController.TradeResponse(false, null, e.getMessage(), null, null);
                }
        }
}
