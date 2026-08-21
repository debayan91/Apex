package io.heron.controller;

import io.heron.market.LatestPrice;
import io.heron.market.LatestPriceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarketDataControllerTest {

    @Mock
    private LatestPriceRepository latestPriceRepository;

    @InjectMocks
    private MarketDataController controller;

    @Test
    void testGetLatestPrices_withSymbol() {
        LatestPrice price = new LatestPrice();
        price.setSymbol("BTCUSDT");
        price.setPrice(new BigDecimal("50000.00"));
        
        when(latestPriceRepository.findById("BTCUSDT")).thenReturn(Optional.of(price));
        
        ResponseEntity<?> response = controller.getLatestPrices("BTCUSDT");
        
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(price, response.getBody());
    }

    @Test
    void testGetLatestPrices_all() {
        LatestPrice price = new LatestPrice();
        price.setSymbol("BTCUSDT");
        price.setPrice(new BigDecimal("50000.00"));
        
        when(latestPriceRepository.findAll()).thenReturn(List.of(price));
        
        ResponseEntity<?> response = controller.getLatestPrices(null);
        
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(List.of(price), response.getBody());
    }
}
