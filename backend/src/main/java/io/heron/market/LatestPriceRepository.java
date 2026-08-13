package io.heron.market;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public interface LatestPriceRepository extends JpaRepository<LatestPrice, String> {

    @Modifying
    @Query(value = "INSERT INTO latest_prices (symbol, price, volume, updated_at) " +
                   "VALUES (:symbol, :price, :volume, now()) " +
                   "ON CONFLICT (symbol) DO UPDATE SET price = EXCLUDED.price, updated_at = now() " +
                   "WHERE latest_prices.price <> EXCLUDED.price", 
           nativeQuery = true)
    void upsertPrice(@Param("symbol") String symbol, 
                     @Param("price") BigDecimal price, 
                     @Param("volume") BigDecimal volume);
}
