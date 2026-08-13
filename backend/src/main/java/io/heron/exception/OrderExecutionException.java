package io.heron.exception;

/**
 * Exception thrown when order execution fails at broker level.
 */
public class OrderExecutionException extends RuntimeException {
    public OrderExecutionException(String message) {
        super(message);
    }

    public OrderExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
