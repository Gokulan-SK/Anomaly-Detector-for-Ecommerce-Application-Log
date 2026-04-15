package com.anomaly.controller;

import com.anomaly.model.User;
import com.anomaly.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * AuthController exposes POST /auth/login for the anomaly detection dashboard.
 *
 * No Spring Security — all endpoints are open.
 * Returns the user object on success, or a 401 with an error message on failure.
 *
 * Note: password is NOT returned in the response for basic safety.
 */
@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * POST /auth/login
     *
     * Request body:
     * {
     *   "username": "analyst1",
     *   "password": "secret"
     * }
     *
     * Response (200 OK):
     * {
     *   "id": 1,
     *   "username": "analyst1",
     *   "role": "ANALYST",
     *   "createdAt": "2026-04-14T..."
     * }
     *
     * Response (401 Unauthorized):
     * {
     *   "error": "Invalid username or password"
     * }
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Username and password are required"));
        }

        try {
            User user = authService.login(username, password);

            // Return user info — deliberately omit password field
            Map<String, Object> response = Map.of(
                    "id", user.getId(),
                    "username", user.getUsername(),
                    "role", user.getRole(),
                    "createdAt", user.getCreatedAt() != null ? user.getCreatedAt().toString() : ""
            );
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException ex) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", ex.getMessage()));
        }
    }
}
