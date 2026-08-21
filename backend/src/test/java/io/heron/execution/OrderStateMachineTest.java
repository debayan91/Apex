package io.heron.execution;

import io.heron.exception.InvalidOrderStateTransitionException;
import io.heron.model.Order;
import io.heron.model.Order.OrderStatus;
import io.heron.model.OrderAuditLog;
import io.heron.repo.OrderAuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderStateMachineTest {

    @Mock
    private OrderAuditLogRepository auditLogRepository;

    @InjectMocks
    private OrderStateMachine stateMachine;

    private Order order;

    @BeforeEach
    void setUp() {
        order = new Order();
        order.setId(1L);
    }

    @Test
    void testValidTransition_PendingToValidated() {
        order.setStatus(OrderStatus.PENDING_VALIDATION);
        
        stateMachine.transition(order, OrderStatus.VALIDATED);
        
        assertEquals(OrderStatus.VALIDATED, order.getStatus());
        
        ArgumentCaptor<OrderAuditLog> captor = ArgumentCaptor.forClass(OrderAuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        
        OrderAuditLog log = captor.getValue();
        assertEquals(1L, log.getOrderId());
        assertEquals(OrderStatus.PENDING_VALIDATION, log.getOldStatus());
        assertEquals(OrderStatus.VALIDATED, log.getNewStatus());
        assertNotNull(log.getTimestamp());
    }

    @Test
    void testValidTransition_ValidatedToFilled() {
        order.setStatus(OrderStatus.VALIDATED);
        
        stateMachine.transition(order, OrderStatus.FILLED);
        
        assertEquals(OrderStatus.FILLED, order.getStatus());
        verify(auditLogRepository, times(1)).save(any(OrderAuditLog.class));
    }

    @Test
    void testInvalidTransition_ThrowsException() {
        order.setStatus(OrderStatus.FILLED);
        
        assertThrows(InvalidOrderStateTransitionException.class, () -> {
            stateMachine.transition(order, OrderStatus.PENDING_VALIDATION);
        });
        
        // Ensure status didn't change
        assertEquals(OrderStatus.FILLED, order.getStatus());
        verify(auditLogRepository, never()).save(any());
    }
}
