package io.heron.service;

import io.heron.exception.InsufficientBalanceException;
import io.heron.model.TransactionLedger;
import io.heron.model.TransactionType;
import io.heron.model.User;
import io.heron.model.Wallet;
import io.heron.repo.TransactionLedgerRepository;
import io.heron.repo.UserRepository;
import io.heron.repo.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private TransactionLedgerRepository ledgerRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private WalletService walletService;

    private Wallet wallet;
    private User user;

    @BeforeEach
    void setUp() {
        wallet = new Wallet(1L, new BigDecimal("1000.00"));
        user = new User();
        user.setId(1L);
        user.setBalance(new BigDecimal("1000.00"));
    }

    @Test
    void testAdjustBalance_SuccessfulDeposit() {
        when(walletRepository.findWalletByUserId(1L)).thenReturn(Optional.of(wallet));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        walletService.adjustBalance(1L, new BigDecimal("500.00"), TransactionType.DEPOSIT);

        assertEquals(new BigDecimal("1500.00"), wallet.getBalance());
        assertEquals(new BigDecimal("1500.00"), user.getBalance());
        verify(ledgerRepository).save(any(TransactionLedger.class));
        verify(walletRepository).save(wallet);
        verify(userRepository).save(user);
    }

    @Test
    void testAdjustBalance_SuccessfulWithdrawal() {
        when(walletRepository.findWalletByUserId(1L)).thenReturn(Optional.of(wallet));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        walletService.adjustBalance(1L, new BigDecimal("-500.00"), TransactionType.WITHDRAWAL);

        assertEquals(new BigDecimal("500.00"), wallet.getBalance());
        assertEquals(new BigDecimal("500.00"), user.getBalance());
        verify(ledgerRepository).save(any(TransactionLedger.class));
        verify(walletRepository).save(wallet);
        verify(userRepository).save(user);
    }

    @Test
    void testAdjustBalance_InsufficientFunds_ThrowsException() {
        when(walletRepository.findWalletByUserId(1L)).thenReturn(Optional.of(wallet));

        assertThrows(InsufficientBalanceException.class, () -> {
            walletService.adjustBalance(1L, new BigDecimal("-1500.00"), TransactionType.WITHDRAWAL);
        });

        // Balance should not have changed
        assertEquals(new BigDecimal("1000.00"), wallet.getBalance());
        verify(ledgerRepository, never()).save(any());
        verify(walletRepository, never()).save(any());
    }

    @Test
    void testGetWallet_NotFound_ThrowsException() {
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            walletService.getWallet(1L);
        });
    }
}
