package io.heron.config;

import io.heron.market.LatestPrice;
import io.heron.market.LatestPriceRepository;
import io.heron.model.User;
import io.heron.model.Wallet;
import io.heron.repo.UserRepository;
import io.heron.repo.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final LatestPriceRepository latestPriceRepository;

    @Override
    public void run(String... args) throws Exception {
        // Seed default user if none exists
        // Seed or top up default user
        BigDecimal initialBalance = new BigDecimal("1000000.00");
        User user = userRepository.findById(1L).orElseGet(() -> User.builder()
                .id(1L)
                .username("pro_trader")
                .email("trader@heron.io")
                .balance(initialBalance)
                .build());
        user.setBalance(initialBalance);
        userRepository.save(user);

        Wallet wallet = walletRepository.findByUserId(1L).orElseGet(() -> Wallet.builder()
                .userId(1L)
                .balance(initialBalance)
                .build());
        wallet.setBalance(initialBalance);
        walletRepository.save(wallet);

        log.info("[HERON] Default User 1 balance initialized to $1,000,000.00.");

        // Seed initial market prices if empty
        if (latestPriceRepository.count() == 0) {
            log.info("[HERON] Seeding initial market prices...");
            latestPriceRepository.save(LatestPrice.builder()
                    .symbol("BTCUSDT")
                    .price(new BigDecimal("65432.10"))
                    .volume(new BigDecimal("125.5"))
                    .updatedAt(LocalDateTime.now())
                    .build());

            latestPriceRepository.save(LatestPrice.builder()
                    .symbol("ETHUSDT")
                    .price(new BigDecimal("3450.75"))
                    .volume(new BigDecimal("840.2"))
                    .updatedAt(LocalDateTime.now())
                    .build());

            log.info("[HERON] Initial prices seeded for BTCUSDT and ETHUSDT.");
        }
    }
}
