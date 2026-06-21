package com.example.Apex.client;

import com.example.Apex.model.Order;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Random;

/**
 * Mock implementation of BrokerClient.
 * Returns dummy prices and simulates successful executions.
 * Replace with real broker integration in production.
 */
@Slf4j
@Component
public class MockBrokerClient implements BrokerClient {

    private final Random random = new Random();

    public BigDecimal getPrice(String symbol) {
        // Generate a random price between 50 and 500
        double price = 50 + (random.nextDouble() * 450);
        BigDecimal result = BigDecimal.valueOf(price).setScale(2, java.math.RoundingMode.HALF_UP);
        log.info("Mock broker: fetched price for {} = {}", symbol, result);
        return result;
    }

    @Override
    public String executeOrder(Order order) {
        log.info("Mock broker: executing order {} {} shares of {} at {}",
                order.getSide(), order.getQuantity(), order.getSymbol(), order.getPrice());
        return "MOCK_EXECUTION_ID_" + System.currentTimeMillis();
    }
}
