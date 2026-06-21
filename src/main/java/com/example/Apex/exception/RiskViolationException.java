package com.example.Apex.exception;

/**
 * Exception thrown when a trade violates risk management rules.
 */
public class RiskViolationException extends RuntimeException {
    public RiskViolationException(String message) {
        super(message);
    }
}
