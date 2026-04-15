package com.anomaly.repository;

import com.anomaly.model.AnomalyEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * AnomalyRepository provides CRUD access to {@link AnomalyEvent} entities.
 *
 * Spring Data JPA automatically implements standard operations:
 *   - save(), findById(), findAll(), deleteById(), etc.
 */
@Repository
public interface AnomalyRepository extends JpaRepository<AnomalyEvent, Long> {

    /**
     * Returns all anomalies associated with a given username.
     * Used by the user-based filtering endpoint: GET /reports/user/{username}
     */
    List<AnomalyEvent> findByUsername(String username);
}

