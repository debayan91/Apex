package com.example.Apex.market;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MarketTickRepository extends JpaRepository<MarketTick, Long> {
    Optional<MarketTick> findTopBySymbolOrderByTimestampDesc(String symbol);
}
