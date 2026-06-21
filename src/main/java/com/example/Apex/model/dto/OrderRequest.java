package com.example.Apex.model.dto;

import com.example.Apex.model.Order;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OrderRequest {
    @NotNull
    private Long userId;

    @NotBlank
    private String symbol;

    @NotNull
    private Order.Side side;

    @NotNull
    @Min(1)
    private Integer quantity;
}
