package io.heron.service;

import io.heron.model.TransactionLedger;
import io.heron.model.TransactionType;
import io.heron.model.Wallet;
import io.heron.repo.TransactionLedgerRepository;
import io.heron.repo.UserRepository;
import io.heron.repo.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@Slf4j
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final TransactionLedgerRepository ledgerRepository;
    private final UserRepository userRepository;

    @Transactional
    public void adjustBalance(Long userId, BigDecimal amount, TransactionType type) {
        log.info("Adjusting balance for user: {}, amount: {}, type: {}", userId, amount, type);

        // 1. Acquire PESSIMISTIC Lock
        Wallet wallet = walletRepository.findWalletByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Wallet not found for user: " + userId));

        // 2. Validate funds (for withdrawals or trades that decrease balance)
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            BigDecimal newBalance = wallet.getBalance().add(amount);
            if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
                throw new io.heron.exception.InsufficientBalanceException("Insufficient funds for user: " + userId + ". Available: $" + wallet.getBalance() + ", Required: $" + amount.abs());
            }
        }

        // 3. Insert record into TransactionLedger
        TransactionLedger ledgerEntry = TransactionLedger.builder()
                .userId(userId)
                .amount(amount)
                .type(type)
                .build();
        ledgerRepository.save(ledgerEntry);

        // 4. Update Wallet balance and User balance
        wallet.setBalance(wallet.getBalance().add(amount));
        walletRepository.save(wallet);

        userRepository.findById(userId).ifPresent(u -> {
            u.setBalance(wallet.getBalance());
            userRepository.save(u);
        });

        log.info("Balance adjusted successfully. New Balance: {}", wallet.getBalance());
    }

    public Wallet getWallet(Long userId) {
        return walletRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Wallet not found for user: " + userId));
    }
}
