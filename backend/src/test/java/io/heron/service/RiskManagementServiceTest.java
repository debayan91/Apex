package io.heron.service;

import io.heron.model.Order;
import io.heron.repo.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RiskManagementServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private RiskManagementService riskManagementService;

    private final Long USER_ID = 1L;
    private final String SYMBOL = "BTC";

    @Test
    void testValidateOrder_Successful() {
        when(orderRepository.countByUserIdAndSymbolAndStatus(USER_ID, SYMBOL, Order.OrderStatus.PENDING_VALIDATION)).thenReturn(0L);

        assertDoesNotThrow(() -> {
            riskManagementService.validateOrder(USER_ID, SYMBOL, 10, new BigDecimal("1000"));
        });
    }

    @Test
    void testValidateOrder_MaxOrderValueExceeded_ThrowsException() {
        assertThrows(RiskManagementService.RiskException.class, () -> {
            riskManagementService.validateOrder(USER_ID, SYMBOL, 1000, new BigDecimal("2000"));
        });
    }

    @Test
    void testValidateOrder_WashTrade_ThrowsException() {
        when(orderRepository.countByUserIdAndSymbolAndStatus(USER_ID, SYMBOL, Order.OrderStatus.PENDING_VALIDATION)).thenReturn(2L);

        assertThrows(RiskManagementService.RiskException.class, () -> {
            riskManagementService.validateOrder(USER_ID, SYMBOL, 10, new BigDecimal("1000"));
        });
    }
}
