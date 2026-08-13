package io.heron.config;

import io.heron.client.BrokerClient;
import io.heron.market.LatestPrice;
import io.heron.market.LatestPriceRepository;
import io.heron.model.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

/**
 * Configuration for external client beans.
 * Plugs in actual database/market data feeds.
 */
@Configuration
@RequiredArgsConstructor
public class ClientConfig {

    private final LatestPriceRepository latestPriceRepository;

    /**
     * Default broker client.
     */
    @Bean
    public BrokerClient brokerClient() {
        return new BrokerClient() {
            @Override
            public BigDecimal getPrice(String symbol) {
                return latestPriceRepository.findById(symbol)
                        .map(LatestPrice::getPrice)
                        .orElseThrow(() -> new RuntimeException("No actual market data available in DB for symbol: " + symbol));
            }

            @Override
            public String executeOrder(Order order) {
                // Return a real local order ID reference instead of a simulated ID
                return "LOCAL_EXEC_" + order.getId();
            }
        };
    }
}
