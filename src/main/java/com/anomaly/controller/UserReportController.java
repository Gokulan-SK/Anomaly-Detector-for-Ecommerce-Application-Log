package com.anomaly.controller;

import com.anomaly.model.AnomalyEvent;
import com.anomaly.repository.AnomalyRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * UserReportController adds user-based anomaly filtering to the /reports namespace.
 *
 * Deliberately separate from ReportController to avoid any risk of breaking
 * the existing reporting endpoints (/summary, /trend, /critical, /endpoints, /export).
 *
 * New endpoint: GET /reports/user/{username}
 */
@RestController
@RequestMapping("/reports")
@CrossOrigin(origins = "*")
public class UserReportController {

    private final AnomalyRepository anomalyRepository;

    public UserReportController(AnomalyRepository anomalyRepository) {
        this.anomalyRepository = anomalyRepository;
    }

    /**
     * GET /reports/user/{username}
     *
     * Returns all anomaly events associated with the given username.
     * Returns an empty list (not 404) when no anomalies exist for the user.
     *
     * @param username the username to filter by
     * @return list of anomaly events linked to that username
     */
    @GetMapping("/user/{username}")
    public ResponseEntity<List<AnomalyEvent>> getAnomaliesByUser(@PathVariable String username) {
        List<AnomalyEvent> anomalies = anomalyRepository.findByUsername(username);
        return ResponseEntity.ok(anomalies);
    }
}
