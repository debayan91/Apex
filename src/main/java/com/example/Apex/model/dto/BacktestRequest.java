package com.example.Apex.model.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BacktestRequest {
    private String strategyName;
    private LocalDateTime start;
    private LocalDateTime end;
}
