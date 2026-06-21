package com.example.Apex.execution;

import com.example.Apex.exception.InvalidOrderStateTransitionException;
import com.example.Apex.model.Order;
import com.example.Apex.model.Order.OrderStatus;
import com.example.Apex.model.OrderAuditLog;
import com.example.Apex.repo.OrderAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Enforces strictly valid order lifecycle transitions.
 *
 * The transition map is the single source of truth for allowed state changes.
 * No other class is permitted to call order.setStatus() directly.
 *
 * Allowed transitions:
 * PENDING_VALIDATION → VALIDATED
 * PENDING_VALIDATION → REJECTED
 * VALIDATED → FILLED
 * VALIDATED → REJECTED
 *
 * All other transitions throw {@link InvalidOrderStateTransitionException}.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OrderStateMachine {

    private final OrderAuditLogRepository auditLogRepository;

    /**
     * Central transition map — the one and only definition of valid state changes.
     * To add a new transition, add it here. No other class needs to change.
     */
    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS;

    static {
        ALLOWED_TRANSITIONS = new EnumMap<>(OrderStatus.class);
        ALLOWED_TRANSITIONS.put(OrderStatus.PENDING_VALIDATION,
                EnumSet.of(OrderStatus.VALIDATED, OrderStatus.REJECTED));
        ALLOWED_TRANSITIONS.put(OrderStatus.VALIDATED,
                EnumSet.of(OrderStatus.FILLED, OrderStatus.REJECTED));
        // FILLED and REJECTED are terminal — no outgoing transitions
    }

    /**
     * Attempts to transition the given order to the target status.
     *
     * @param order        the order to transition
     * @param targetStatus the desired new status
     * @throws InvalidOrderStateTransitionException if the transition is not in the
     *                                              allowed map
     */
    public void transition(Order order, OrderStatus targetStatus) {
        OrderStatus currentStatus = order.getStatus();
        Set<OrderStatus> allowedTargets = ALLOWED_TRANSITIONS.getOrDefault(currentStatus,
                EnumSet.noneOf(OrderStatus.class));

        if (!allowedTargets.contains(targetStatus)) {
            log.error("[STATE_MACHINE] Illegal transition blocked: Order#{} {} → {}",
                    order.getId(), currentStatus, targetStatus);
            throw new InvalidOrderStateTransitionException(currentStatus, targetStatus);
        }

        log.info("[STATE_MACHINE] Order#{} transitioning: {} → {}", order.getId(), currentStatus, targetStatus);

        // Commit the transition
        order.setStatus(targetStatus);

        // Persist audit trail for every valid transition
        OrderAuditLog auditLog = OrderAuditLog.builder()
                .orderId(order.getId())
                .oldStatus(currentStatus)
                .newStatus(targetStatus)
                .timestamp(LocalDateTime.now())
                .build();
        auditLogRepository.save(auditLog);
    }
}
