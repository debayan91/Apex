package io.heron.controller;

import io.heron.model.User;
import io.heron.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

import io.heron.service.WalletService;
import io.heron.model.TransactionType;
import io.heron.model.Wallet;
import io.heron.repo.WalletRepository;

/**
 * REST controller for user operations.
 * Provides endpoints to create and manage users.
 */
@Slf4j
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final WalletService walletService;
    private final WalletRepository walletRepository;

    /**
     * Create a new user.
     * POST /user/create
     * Body: { "username": "john", "email": "john@example.com", "initialBalance":
     * "10000.00" }
     */
    @PostMapping("/create")
    public ResponseEntity<User> createUser(@Valid @RequestBody CreateUserRequest request) {
        log.info("Creating user: {}", request);

        User user = userService.createUser(request.username(), request.email(), request.initialBalance());
        return ResponseEntity.ok(user);
    }

    /**
     * Get user details.
     * GET /user/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<User> getUser(@PathVariable Long id) {
        log.info("Fetching user {}", id);
        User user = userService.findById(id);
        return ResponseEntity.ok(user);
    }

    @PostMapping("/{id}/deposit")
    public ResponseEntity<Void> deposit(@PathVariable Long id, @RequestBody DepositRequest request) {
        log.info("Depositing {} to user {}", request.amount(), id);
        walletService.adjustBalance(id, request.amount(), TransactionType.DEPOSIT);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/wallet")
    public ResponseEntity<Wallet> getWallet(@PathVariable Long id) {
        Wallet wallet = walletRepository.findWalletByUserId(id).orElseThrow();
        return ResponseEntity.ok(wallet);
    }

    public record DepositRequest(BigDecimal amount) {
    }

    public record CreateUserRequest(
            @NotBlank(message = "Username is required") String username,
            @NotBlank(message = "Email is required") String email,
            BigDecimal initialBalance) {
        public CreateUserRequest {
            if (initialBalance == null) {
                initialBalance = new BigDecimal("10000.00");
            }
        }
    }
}
