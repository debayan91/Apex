package com.example.Apex.exception;

import com.example.Apex.model.Order.OrderStatus;

/**
 * Thrown when an order state transition is attempted that is not permitted
 * by the OrderStateMachine transition map.
 */
public class InvalidOrderStateTransitionException extends RuntimeException {

    private final OrderStatus from;
    private final OrderStatus to;

    public InvalidOrderStateTransitionException(OrderStatus from, OrderStatus to) {
        super(String.format("Invalid order state transition: %s → %s", from, to));
        this.from = from;
        this.to = to;
    }

    public OrderStatus getFrom() {
        return from;
    }

    public OrderStatus getTo() {
        return to;
    }
}
