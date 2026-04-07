package com.anomaly.repository;

import com.anomaly.model.AnomalyEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<AnomalyEvent, Long> {

    @Query("SELECT a.severity, COUNT(a) FROM AnomalyEvent a GROUP BY a.severity")
    List<Object[]> countBySeverity();

    @Query("SELECT a.anomalyType, COUNT(a) FROM AnomalyEvent a GROUP BY a.anomalyType")
    List<Object[]> countByAnomalyType();

    @Query("SELECT a.sourceLayer, COUNT(a) FROM AnomalyEvent a GROUP BY a.sourceLayer")
    List<Object[]> countBySourceLayer();

    @Query("SELECT COUNT(DISTINCT a.sourceLayer) FROM AnomalyEvent a")
    long countDistinctEndpoints();

    List<AnomalyEvent> findTop10BySeverityOrderByCreatedAtDesc(String severity);

    List<AnomalyEvent> findByCreatedAtAfter(Instant time);
}
