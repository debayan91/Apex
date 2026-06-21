package com.example.Apex.portfolio;

import com.example.Apex.client.BrokerClient;
import com.example.Apex.model.Holding;
import com.example.Apex.model.Order;
import com.example.Apex.model.User;
import com.example.Apex.model.dto.PortfolioSummary;
import com.example.Apex.repo.HoldingRepository;
import com.example.Apex.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Service managing user portfolios and holdings.
 * Updates positions after trades and calculates portfolio value.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PortfolioService {

    private final HoldingRepository holdingRepository;
    private final UserRepository userRepository;
    private final BrokerClient brokerClient;

    /**
     * Update user holdings and balance after a trade.
     * For BUY: add to holdings, deduct from balance
     * For SELL: remove from holdings, add to balance
     */
    @Transactional
    public void updatePortfolio(Order order) {
        log.info("Updating portfolio for order {}", order.getId());

        User user = userRepository.findById(order.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found: " + order.getUserId()));

        BigDecimal tradeValue = order.getPrice().multiply(BigDecimal.valueOf(order.getQuantity()));

        if (order.getSide() == Order.Side.BUY) {
            handleBuy(user, order);
            user.setBalance(user.getBalance().subtract(tradeValue));
        } else {
            handleSell(user, order);
            user.setBalance(user.getBalance().add(tradeValue));
        }

        userRepository.save(user);
        log.info("Portfolio updated for user {}", user.getId());
    }

    private void handleBuy(User user, Order order) {
        Optional<Holding> existingHolding = holdingRepository
                .findByUserIdAndSymbol(user.getId(), order.getSymbol());

        if (existingHolding.isPresent()) {
            Holding holding = existingHolding.get();
            // Calculate new average price
            BigDecimal totalCost = holding.getAveragePrice()
                    .multiply(BigDecimal.valueOf(holding.getQuantity()))
                    .add(order.getPrice().multiply(BigDecimal.valueOf(order.getQuantity())));
            int newQuantity = holding.getQuantity() + order.getQuantity();
            BigDecimal newAvgPrice = totalCost.divide(BigDecimal.valueOf(newQuantity), 2,
                    java.math.RoundingMode.HALF_UP);

            holding.setQuantity(newQuantity);
            holding.setAveragePrice(newAvgPrice);
            holdingRepository.save(holding);
        } else {
            Holding newHolding = Holding.builder()
                    .userId(user.getId())
                    .symbol(order.getSymbol())
                    .quantity(order.getQuantity())
                    .averagePrice(order.getPrice())
                    .build();
            holdingRepository.save(newHolding);
        }
    }

    private void handleSell(User user, Order order) {
        Holding holding = holdingRepository
                .findByUserIdAndSymbol(user.getId(), order.getSymbol())
                .orElseThrow(() -> new RuntimeException("No holding found for symbol: " + order.getSymbol()));

        int newQuantity = holding.getQuantity() - order.getQuantity();
        if (newQuantity < 0) {
            throw new RuntimeException("Cannot sell more than held quantity");
        }

        if (newQuantity == 0) {
            holdingRepository.delete(holding);
        } else {
            holding.setQuantity(newQuantity);
            holdingRepository.save(holding);
        }
    }

    /**
     * Get complete portfolio summary for a user.
     */
    public PortfolioSummary getPortfolioSummary(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        List<Holding> holdings = holdingRepository.findByUserId(userId);

        // Calculate total portfolio value
        BigDecimal holdingsValue = holdings.stream()
                .map(h -> {
                    BigDecimal currentPrice = brokerClient.getPrice(h.getSymbol());
                    return currentPrice.multiply(BigDecimal.valueOf(h.getQuantity()));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalValue = user.getBalance().add(holdingsValue);

        return PortfolioSummary.builder()
                .userId(userId)
                .cashBalance(user.getBalance())
                .totalValue(totalValue)
                .holdings(holdings)
                .build();
    }
}
