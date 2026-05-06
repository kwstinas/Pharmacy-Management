package com.pharmacy.controller;

import com.pharmacy.model.User;
import com.pharmacy.repository.UserRepository;
import com.pharmacy.security.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private UserRepository userRepo;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthController authController;

    // ====== REGISTER TESTS ======

    @Test
    @DisplayName("Register with valid data should return 201")
    void register_validData_shouldSucceed() {
        when(userRepo.existsByUsername("testuser")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("$2a$hashed");
        when(userRepo.save(any())).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });
        when(jwtUtil.generateToken(eq(1L), eq("testuser"), eq("PHARMACIST"))).thenReturn("jwt-token");

        var body = Map.of("username", "testuser", "password", "Test@1234");
        var response = authController.register(body);

        assertEquals(201, response.getStatusCode().value());
        assertTrue(response.getBody().success());
        assertEquals("jwt-token", response.getBody().data().get("token"));
    }

    @Test
    @DisplayName("Register with short password should return 400")
    void register_shortPassword_shouldFail() {
        var body = Map.of("username", "testuser", "password", "Ab1!");
        var response = authController.register(body);

        assertEquals(400, response.getStatusCode().value());
        assertFalse(response.getBody().success());
        assertTrue(response.getBody().message().contains("8 characters"));
    }

    @Test
    @DisplayName("Register without uppercase should return 400")
    void register_noUppercase_shouldFail() {
        var body = Map.of("username", "testuser", "password", "test@1234");
        var response = authController.register(body);

        assertEquals(400, response.getStatusCode().value());
        assertFalse(response.getBody().success());
        assertTrue(response.getBody().message().contains("uppercase"));
    }

    @Test
    @DisplayName("Register without symbol should return 400")
    void register_noSymbol_shouldFail() {
        var body = Map.of("username", "testuser", "password", "TestTest1234");
        var response = authController.register(body);

        assertEquals(400, response.getStatusCode().value());
        assertFalse(response.getBody().success());
        assertTrue(response.getBody().message().contains("symbol"));
    }

    @Test
    @DisplayName("Register with existing username should return 400")
    void register_duplicateUsername_shouldFail() {
        when(userRepo.existsByUsername("taken")).thenReturn(true);

        var body = Map.of("username", "taken", "password", "Test@1234");
        var response = authController.register(body);

        assertEquals(400, response.getStatusCode().value());
        assertTrue(response.getBody().message().contains("already exists"));
    }

    @Test
    @DisplayName("Register with password containing username should return 400")
    void register_passwordContainsUsername_shouldFail() {
        when(userRepo.existsByUsername("admin")).thenReturn(false);

        var body = Map.of("username", "admin", "password", "Admin@1234");
        var response = authController.register(body);

        assertEquals(400, response.getStatusCode().value());
        assertTrue(response.getBody().message().contains("username"));
    }

    @Test
    @DisplayName("Register with invalid username chars should return 400")
    void register_invalidUsername_shouldFail() {
        var body = Map.of("username", "test user!", "password", "Test@1234");
        var response = authController.register(body);

        assertEquals(400, response.getStatusCode().value());
        assertTrue(response.getBody().message().contains("letters, numbers"));
    }

    // ====== LOGIN TESTS ======

    @Test
    @DisplayName("Login with correct credentials should return token")
    void login_correctCredentials_shouldSucceed() {
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setPassword("$2a$hashed");
        user.setRole("PHARMACIST");

        when(userRepo.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Test@1234", "$2a$hashed")).thenReturn(true);
        when(jwtUtil.generateToken(1L, "testuser", "PHARMACIST")).thenReturn("jwt-token");

        var body = Map.of("username", "testuser", "password", "Test@1234");
        var response = authController.login(body);

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().success());
        assertEquals("jwt-token", response.getBody().data().get("token"));
    }

    @Test
    @DisplayName("Login with wrong password should return 401")
    void login_wrongPassword_shouldFail() {
        User user = new User();
        user.setUsername("testuser");
        user.setPassword("$2a$hashed");

        when(userRepo.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "$2a$hashed")).thenReturn(false);

        var body = Map.of("username", "testuser", "password", "wrong");
        var response = authController.login(body);

        assertEquals(401, response.getStatusCode().value());
        assertTrue(response.getBody().message().contains("Invalid"));
    }

    @Test
    @DisplayName("Login with non-existent user should return 401")
    void login_userNotFound_shouldFail() {
        when(userRepo.findByUsername("nobody")).thenReturn(Optional.empty());

        var body = Map.of("username", "nobody", "password", "Test@1234");
        var response = authController.login(body);

        assertEquals(401, response.getStatusCode().value());
    }
}