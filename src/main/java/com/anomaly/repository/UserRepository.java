package com.anomaly.repository;

import com.anomaly.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * UserRepository provides CRUD access to {@link User} entities.
 * Used exclusively by AuthService for login lookups.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Finds a user by their unique username.
     *
     * @param username the username to search for
     * @return an Optional containing the user if found, or empty if not
     */
    Optional<User> findByUsername(String username);
}
