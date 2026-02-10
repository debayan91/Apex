package com.example.Apex.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for trade execution results.
 * Returns success status and execution details to client.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradeResponse {

    private boolean success;
    private Long orderId;
    private String message;
    private BigDecimal executedPrice;
    private String details;
}
