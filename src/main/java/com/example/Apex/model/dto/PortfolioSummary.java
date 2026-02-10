package com.example.Apex.model.dto;

import com.example.Apex.model.Holding;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO for portfolio summary view.
 * Aggregates user balance and all holdings.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioSummary {

    private Long userId;
    private BigDecimal cashBalance;
    private BigDecimal totalValue;
    private List<Holding> holdings;
}
