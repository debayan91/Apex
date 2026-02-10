package com.example.Apex.client;

import com.example.Apex.model.Order;

import java.math.BigDecimal;

/**
 * Interface for broker integration.
 * Implementations handle real-time price fetching and order execution.
 */
public interface BrokerClient {

    /**
     * Fetch current market price for a symbol.
     * 
     * @param symbol the stock/crypto symbol
     * @return current price
     */
    BigDecimal getPrice(String symbol);

    /**
     * Execute an order with the broker.
     * 
     * @param order the order to execute
     * @return execution confirmation details
     */
    String executeOrder(Order order);
}
