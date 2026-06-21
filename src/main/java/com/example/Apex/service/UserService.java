package com.example.Apex.service;

import com.example.Apex.model.User;
import com.example.Apex.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Service for user management operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

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
