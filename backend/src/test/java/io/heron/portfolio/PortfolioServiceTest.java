package io.heron.portfolio;

import io.heron.controller.PortfolioController;
import io.heron.market.MarketDataService;
import io.heron.model.Holding;
import io.heron.model.Order;
import io.heron.model.User;
import io.heron.model.Wallet;
import io.heron.repo.HoldingRepository;
import io.heron.repo.UserRepository;
import io.heron.repo.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PortfolioServiceTest {

    @Mock
    private HoldingRepository holdingRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private MarketDataService marketDataService;

    @InjectMocks
    private PortfolioService portfolioService;

    private User user;
    private Order buyOrder;
    private Order sellOrder;
    private Holding existingHolding;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setBalance(new BigDecimal("10000.00"));

        buyOrder = new Order();
        buyOrder.setId(10L);
        buyOrder.setUserId(1L);
        buyOrder.setSymbol("BTC");
        buyOrder.setSide(Order.Side.BUY);
        buyOrder.setQuantity(2);
        buyOrder.setPrice(new BigDecimal("1000.00"));

        sellOrder = new Order();
        sellOrder.setId(11L);
        sellOrder.setUserId(1L);
        sellOrder.setSymbol("BTC");
        sellOrder.setSide(Order.Side.SELL);
        sellOrder.setQuantity(1);
        sellOrder.setPrice(new BigDecimal("1500.00"));

        existingHolding = new Holding();
        existingHolding.setId(100L);
        existingHolding.setUserId(1L);
        existingHolding.setSymbol("BTC");
        existingHolding.setQuantity(1);
        existingHolding.setAveragePrice(new BigDecimal("500.00"));
    }

    @Test
    void testUpdatePortfolio_Buy_NewHolding() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(holdingRepository.findByUserIdAndSymbol(1L, "BTC")).thenReturn(Optional.empty());

        portfolioService.updatePortfolio(buyOrder);

        assertEquals(new BigDecimal("8000.00"), user.getBalance());
        verify(holdingRepository).save(any(Holding.class));
        verify(userRepository).save(user);
    }

    @Test
    void testUpdatePortfolio_Buy_ExistingHolding() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(holdingRepository.findByUserIdAndSymbol(1L, "BTC")).thenReturn(Optional.of(existingHolding));

        portfolioService.updatePortfolio(buyOrder);

        assertEquals(new BigDecimal("8000.00"), user.getBalance());
        // Existing holding logic: 1 @ 500 + 2 @ 1000 = total cost 2500. avg = 2500 / 3 = 833.33
        assertEquals(3, existingHolding.getQuantity());
        assertEquals(new BigDecimal("833.33"), existingHolding.getAveragePrice());
        verify(holdingRepository).save(existingHolding);
        verify(userRepository).save(user);
    }

    @Test
    void testUpdatePortfolio_Sell_Partial() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        existingHolding.setQuantity(2);
        when(holdingRepository.findByUserIdAndSymbol(1L, "BTC")).thenReturn(Optional.of(existingHolding));

        portfolioService.updatePortfolio(sellOrder);

        assertEquals(new BigDecimal("11500.00"), user.getBalance());
        assertEquals(1, existingHolding.getQuantity());
        verify(holdingRepository).save(existingHolding);
        verify(userRepository).save(user);
    }

    @Test
    void testUpdatePortfolio_Sell_Full() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(holdingRepository.findByUserIdAndSymbol(1L, "BTC")).thenReturn(Optional.of(existingHolding));

        portfolioService.updatePortfolio(sellOrder);

        assertEquals(new BigDecimal("11500.00"), user.getBalance());
        verify(holdingRepository).delete(existingHolding);
        verify(userRepository).save(user);
    }

    @Test
    void testGetPortfolioSummary() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        Wallet wallet = new Wallet(1L, new BigDecimal("5000.00"));
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(wallet));
        
        Holding h = new Holding();
        h.setSymbol("BTC");
        h.setQuantity(2);
        when(holdingRepository.findByUserId(1L)).thenReturn(List.of(h));
        when(marketDataService.getLatestPrice("BTC")).thenReturn(new BigDecimal("2000.00"));

        PortfolioController.PortfolioSummary summary = portfolioService.getPortfolioSummary(1L);

        assertEquals(new BigDecimal("5000.00"), summary.cashBalance());
        assertEquals(new BigDecimal("9000.00"), summary.totalValue()); // 5000 + 2 * 2000
    }
}
