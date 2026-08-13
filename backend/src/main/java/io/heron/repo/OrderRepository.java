package io.heron.repo;

import io.heron.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Order entity operations.
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUserId(Long userId);

    List<Order> findByStatus(Order.OrderStatus status);

    Optional<Order> findByIdempotencyKey(String idempotencyKey);

    long countByUserIdAndSymbolAndStatus(Long userId, String symbol, Order.OrderStatus status);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.userId = :userId AND o.createdAt >= :startDate")
    long countOrdersByUserIdSince(Long userId, LocalDateTime startDate);
}
