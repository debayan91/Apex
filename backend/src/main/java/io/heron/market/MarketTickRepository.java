package io.heron.market;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MarketTickRepository extends JpaRepository<MarketTick, Long> {


    List<MarketTick> findByTimestampBetweenOrderByTimestampAsc(LocalDateTime start, LocalDateTime end);
}
