package com.anomaly.service;

import com.anomaly.model.AnomalyEvent;
import com.anomaly.repository.AnomalyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * AnomalyService provides the data-access layer for {@link AnomalyEvent} operations.
 *
 * Responsibilities:
 *  - Save newly detected anomalies to the H2 database
 *  - Retrieve anomalies for querying and reporting
 *
 * This service acts as the persistence facade between the detection engine
 * and the repository layer.
 *
 * No business logic is implemented yet — skeleton only.
 */
@Service
public class AnomalyService {

    private static final Logger log = LoggerFactory.getLogger(AnomalyService.class);

    private final AnomalyRepository anomalyRepository;

    public AnomalyService(AnomalyRepository anomalyRepository) {
        this.anomalyRepository = anomalyRepository;
    }

    /**
     * Persists a detected {@link AnomalyEvent} to the database.
     *
     * @param anomalyEvent the anomaly to save
     * @return the saved entity with its generated ID and createdAt timestamp
     */
    public AnomalyEvent save(AnomalyEvent anomalyEvent) {
        log.info("AnomalyService.save() called for anomaly: {}", anomalyEvent);
        return anomalyRepository.save(anomalyEvent);
    }

    /**
     * Retrieves all anomaly events from the database.
     *
     * @return list of all stored anomaly events
     */
    public List<AnomalyEvent> findAll() {
        log.info("AnomalyService.findAll() called.");
        return anomalyRepository.findAll();
    }

    /**
     * Retrieves a single anomaly event by its ID.
     *
     * @param id the ID of the anomaly event
     * @return an Optional containing the found event, or empty if not found
     */
    public Optional<AnomalyEvent> findById(Long id) {
        log.info("AnomalyService.findById() called with id: {}", id);
        return anomalyRepository.findById(id);
    }
}
