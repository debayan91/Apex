package com.example.Apex.risk;

import com.example.Apex.exception.InsufficientBalanceException;
import com.example.Apex.exception.RiskViolationException;
import com.example.Apex.model.User;
import com.example.Apex.repo.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Risk management service.
 * Validates trades against risk rules before execution.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RiskGuard {

    private final OrderRepository orderRepository;

    private static final BigDecimal MIN_BALANCE = new BigDecimal("100.00");
    private static final int MAX_DAILY_TRADES = 50;

    /**
     * Validate if a trade passes all risk checks.
     * 
     * @throws InsufficientBalanceException if user lacks funds
     * @throws RiskViolationException       if risk limits are breached
     */
    public void validateTrade(User user, BigDecimal tradeValue, int quantity) {
        log.info("Risk check: userId={}, balance={}, tradeValue={}, quantity={}",
                user.getId(), user.getBalance(), tradeValue, quantity);

        // Check sufficient balance
        if (user.getBalance().compareTo(tradeValue) < 0) {
            throw new InsufficientBalanceException(
                    String.format("Insufficient balance. Required: %s, Available: %s",
                            tradeValue, user.getBalance()));
        }

        // Check minimum balance after trade
        BigDecimal balanceAfterTrade = user.getBalance().subtract(tradeValue);
        if (balanceAfterTrade.compareTo(MIN_BALANCE) < 0) {
            throw new RiskViolationException(
                    String.format("Trade would violate minimum balance requirement of %s", MIN_BALANCE));
        }

        // Check daily trade limit
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        long todayTradeCount = orderRepository.countOrdersByUserIdSince(user.getId(), startOfDay);
        if (todayTradeCount >= MAX_DAILY_TRADES) {
            throw new RiskViolationException(
                    String.format("Daily trade limit of %d exceeded. Current count: %d",
                            MAX_DAILY_TRADES, todayTradeCount));
        }

        log.info("Risk check PASSED for user {}", user.getId());
    }
}
