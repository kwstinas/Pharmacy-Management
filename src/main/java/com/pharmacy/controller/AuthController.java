package com.pharmacy.controller;

import com.pharmacy.dto.Dtos.ApiResponse;
import com.pharmacy.model.User;
import com.pharmacy.repository.UserRepository;
import com.pharmacy.security.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepo;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserRepository userRepo, JwtUtil jwtUtil,
                          PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Map<String, String>>> register(
            @RequestBody Map<String, String> body) {

        String username = body.get("username");
        String password = body.get("password");

        // Username validation
        if (username == null || username.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Username is required"));
        }

        if (username.length() < 3 || username.length() > 30) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Username must be 3-30 characters"));
        }

        if (!username.matches("^[a-zA-Z0-9_]+$")) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Username can only contain letters, numbers and underscores"));
        }

        if (userRepo.existsByUsername(username)) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Username already exists"));
        }

        // Password validation
        List<String> passwordErrors = validatePassword(password);
        if (!passwordErrors.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(String.join("; ", passwordErrors)));
        }

        // Check password doesn't contain username
        if (password.toLowerCase().contains(username.toLowerCase())) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Password cannot contain your username"));
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole("PHARMACIST");
        userRepo.save(user);

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Registration successful", Map.of(
                        "token", token,
                        "username", user.getUsername(),
                        "role", user.getRole()
                )));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, String>>> login(
            @RequestBody Map<String, String> body) {

        String username = body.get("username");
        String password = body.get("password");

        if (username == null || password == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Username and password are required"));
        }

        User user = userRepo.findByUsername(username).orElse(null);

        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Invalid username or password"));
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());

        return ResponseEntity.ok(ApiResponse.ok("Login successful", Map.of(
                "token", token,
                "username", user.getUsername(),
                "role", user.getRole()
        )));
    }

    private List<String> validatePassword(String password) {
        List<String> errors = new ArrayList<>();

        if (password == null || password.isBlank()) {
            errors.add("Password is required");
            return errors;
        }

        if (password.length() < 8) {
            errors.add("At least 8 characters");
        }

        if (password.length() > 100) {
            errors.add("Maximum 100 characters");
        }

        if (!password.matches(".*[a-z].*")) {
            errors.add("At least one lowercase letter");
        }

        if (!password.matches(".*[A-Z].*")) {
            errors.add("At least one uppercase letter");
        }

        if (!password.matches(".*[0-9].*")) {
            errors.add("At least one number");
        }

        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) {
            errors.add("At least one symbol (!@#$%^&*...)");
        }

        // Common passwords check
        List<String> common = List.of("password", "12345678", "qwerty12", "admin123", "letmein1");
        if (common.contains(password.toLowerCase())) {
            errors.add("This password is too common");
        }

        return errors;
    }
}