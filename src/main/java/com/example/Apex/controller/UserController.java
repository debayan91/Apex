package com.example.Apex.controller;

import com.example.Apex.model.User;
import com.example.Apex.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

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

    /**
     * Create a new user.
     * POST /user/create
     * Body: { "username": "john", "email": "john@example.com", "initialBalance":
     * "10000.00" }
     */
    @PostMapping("/create")
    public ResponseEntity<User> createUser(@RequestBody Map<String, String> request) {
        log.info("Creating user: {}", request);

        String username = request.get("username");
        String email = request.get("email");
        BigDecimal initialBalance = new BigDecimal(request.getOrDefault("initialBalance", "10000.00"));

        User user = userService.createUser(username, email, initialBalance);
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
}
