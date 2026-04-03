package com.anomaly.repository;

import com.anomaly.model.AnomalyEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * AnomalyRepository provides CRUD access to {@link AnomalyEvent} entities.
 *
 * Spring Data JPA automatically implements standard operations:
 *   - save(), findById(), findAll(), deleteById(), etc.
 *
 * Custom query methods will be added here in future phases
 * as detection and reporting logic is implemented.
 */
@Repository
public interface AnomalyRepository extends JpaRepository<AnomalyEvent, Long> {

    // No custom queries yet — skeleton only.
    // Future examples:
    //   List<AnomalyEvent> findBySeverity(String severity);
    //   List<AnomalyEvent> findBySourceLayer(String sourceLayer);
    //   List<AnomalyEvent> findBySessionId(String sessionId);
}
