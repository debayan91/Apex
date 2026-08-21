package io.heron.market;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@RedisHash("LatestPrice")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LatestPrice implements Serializable {

    @Id
    private String symbol;

    private BigDecimal price;

    private BigDecimal volume; 

    private LocalDateTime updatedAt;
}
