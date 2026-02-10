package com.example.Apex.exception;

/**
 * Exception thrown when user has insufficient balance for a trade.
 */
public class InsufficientBalanceException extends RuntimeException {
    public InsufficientBalanceException(String message) {
        super(message);
    }
}
