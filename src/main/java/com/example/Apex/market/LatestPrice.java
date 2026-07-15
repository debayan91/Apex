package com.example.Apex.market;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "latest_prices")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LatestPrice {

    @Id
    @Column(name = "symbol", nullable = false, unique = true)
    private String symbol;

    @Column(name = "price", nullable = false, precision = 19, scale = 4)
    private BigDecimal price;

    @Column(name = "volume")
    private BigDecimal volume; 

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
