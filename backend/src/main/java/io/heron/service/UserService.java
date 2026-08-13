package io.heron.service;

import io.heron.model.User;
import io.heron.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.heron.repo.WalletRepository;
import io.heron.model.Wallet;

import java.math.BigDecimal;

/**
 * Service for user management operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;

    /**
     * Create a new user with initial balance.
     */
    @Transactional
    public User createUser(String username, String email, BigDecimal initialBalance) {
        log.info("Creating user: username={}, email={}", username, email);

        User user = User.builder()
                .username(username)
                .email(email)
                .balance(initialBalance)
                .build();

        User savedUser = userRepository.save(user);
        walletRepository.save(new Wallet(savedUser.getId(), initialBalance));
        log.info("User created: id={}", savedUser.getId());
        return savedUser;
    }

    /**
     * Find user by ID.
     */
    public User findById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
    }

    /**
     * Update user balance.
     */
    @Transactional
    public User updateBalance(Long userId, BigDecimal newBalance) {
        User user = findById(userId);
        user.setBalance(newBalance);
        return userRepository.save(user);
    }
}
