package com.example.Apex.repo;

import com.example.Apex.model.TradeLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for TradeLog entity operations.
 */
@Repository
public interface TradeLogRepository extends JpaRepository<TradeLog, Long> {

    List<TradeLog> findByOrderId(Long orderId);
}
