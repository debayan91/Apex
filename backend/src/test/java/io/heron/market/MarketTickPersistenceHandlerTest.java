package io.heron.market;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MarketTickPersistenceHandlerTest {

    @Mock
    private MarketTickRepository marketTickRepository;

    @Mock
    private LatestPriceRepository latestPriceRepository;

    @InjectMocks
    private MarketTickPersistenceHandler handler;

    @Test
    void testPersist() {
        BinanceTradeEvent event = new BinanceTradeEvent();
        event.setEventType("trade");
        event.setSymbol("BTCUSDT");
        event.setPrice(new BigDecimal("50000.00"));
        event.setEventTime(Instant.now().toEpochMilli());
        
        handler.persist(event);
        
        ArgumentCaptor<LatestPrice> captor = ArgumentCaptor.forClass(LatestPrice.class);
        verify(latestPriceRepository).save(captor.capture());
        
        LatestPrice saved = captor.getValue();
        assertEquals("BTCUSDT", saved.getSymbol());
        assertEquals(new BigDecimal("50000.00"), saved.getPrice());
        assertNotNull(saved.getUpdatedAt());
        
        verify(marketTickRepository).save(any(MarketTick.class));
    }
}
