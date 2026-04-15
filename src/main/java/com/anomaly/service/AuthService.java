package com.anomaly.service;

import com.anomaly.model.User;
import com.anomaly.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * AuthService handles authentication for the anomaly detection portal.
 *
 * Design:
 *  - Plain-text password comparison (no hashing, no JWT, no Spring Security)
 *  - Throws IllegalArgumentException on invalid credentials (caller maps to 401)
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Authenticates a user by username and plain-text password.
     *
     * @param username the username provided at login
     * @param password the plain-text password provided at login
     * @return the authenticated {@link User} object
     * @throws IllegalArgumentException if the username does not exist or the password is wrong
     */
    public User login(String username, String password) {
        log.info("AuthService.login() called for username: {}", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("Login failed — username not found: {}", username);
                    return new IllegalArgumentException("Invalid username or password");
                });

        if (!user.getPassword().equals(password)) {
            log.warn("Login failed — wrong password for username: {}", username);
            throw new IllegalArgumentException("Invalid username or password");
        }

        log.info("Login successful for user: {} (role: {})", username, user.getRole());
        return user;
    }
}
