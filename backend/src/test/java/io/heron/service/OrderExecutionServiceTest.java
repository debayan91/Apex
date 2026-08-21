package io.heron.service;

import io.heron.execution.OrderStateMachine;
import io.heron.market.MarketDataService;
import io.heron.model.Holding;
import io.heron.model.Order;
import io.heron.model.Wallet;
import io.heron.repo.HoldingRepository;
import io.heron.repo.OrderRepository;
import io.heron.strategy.StrategySignal;
import io.heron.strategy.TradingStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderExecutionServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private RiskManagementService riskManagementService;
    @Mock
    private WalletService walletService;
    @Mock
    private MarketDataService marketDataService;
    @Mock
    private OrderStateMachine orderStateMachine;
    @Mock
    private HoldingRepository holdingRepository;

    private Map<String, TradingStrategy> strategies = new HashMap<>();

    private OrderExecutionService orderExecutionService;

    @Mock
    private TradingStrategy mockStrategy;

    @BeforeEach
    void setUp() {
        strategies.put("SMA", mockStrategy);
        orderExecutionService = new OrderExecutionService(
                orderRepository, riskManagementService, walletService,
                marketDataService, orderStateMachine, holdingRepository, strategies);
        
        lenient().doAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            Order.OrderStatus status = invocation.getArgument(1);
            o.setStatus(status);
            return null;
        }).when(orderStateMachine).transition(any(Order.class), any(Order.OrderStatus.class));
    }

    @Test
    void testExecuteOrder_Idempotency() {
        Order existingOrder = new Order();
        existingOrder.setId(100L);
        when(orderRepository.findByIdempotencyKey("test-key")).thenReturn(Optional.of(existingOrder));

        Order result = orderExecutionService.executeOrder(1L, "BTC", Order.Side.BUY, 1, "test-key", null);

        assertEquals(100L, result.getId());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void testExecuteOrder_SuccessfulBuy() {
        when(orderRepository.findByIdempotencyKey("test-key")).thenReturn(Optional.empty());
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> {
            Order o = i.getArgument(0);
            if (o.getId() == null) o.setId(1L);
            return o;
        });
        when(marketDataService.getLatestPrice("BTC")).thenReturn(new BigDecimal("1000.00"));
        
        Wallet wallet = new Wallet(1L, new BigDecimal("2000.00"));
        when(walletService.getWallet(1L)).thenReturn(wallet);

        Order result = orderExecutionService.executeOrder(1L, "BTC", Order.Side.BUY, 1, "test-key", null);

        assertEquals(Order.OrderStatus.FILLED, result.getStatus());
        assertEquals(new BigDecimal("1000.00"), result.getExecutionPrice());
        verify(orderStateMachine).transition(any(), eq(Order.OrderStatus.VALIDATED));
        verify(orderStateMachine).transition(any(), eq(Order.OrderStatus.FILLED));
        verify(walletService).adjustBalance(eq(1L), eq(new BigDecimal("-1000.00")), any());
        verify(holdingRepository).save(any(Holding.class));
    }

    @Test
    void testExecuteOrder_StrategyRejection() {
        when(orderRepository.findByIdempotencyKey("test-key")).thenReturn(Optional.empty());
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));
        when(marketDataService.getLatestPrice("BTC")).thenReturn(new BigDecimal("1000.00"));

        StrategySignal signal = new StrategySignal("SMA", StrategySignal.SignalType.SELL, 0.8, "Sell it");
        when(mockStrategy.analyze(any(), any())).thenReturn(signal);

        Order result = orderExecutionService.executeOrder(1L, "BTC", Order.Side.BUY, 1, "test-key", "SMA");

        assertEquals(Order.OrderStatus.REJECTED, result.getStatus());
        verify(orderStateMachine).transition(any(), eq(Order.OrderStatus.REJECTED));
        verify(walletService, never()).adjustBalance(any(), any(), any());
    }
}
