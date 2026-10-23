package com.ruralhealthcare.ai.repository;

import com.ruralhealthcare.ai.domain.AiPredictionResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AiPredictionRepository extends JpaRepository<AiPredictionResult, UUID> {

    Optional<AiPredictionResult> findFirstByCaseIdOrderByCreatedAtDesc(UUID caseId);

    List<AiPredictionResult> findByCaseIdOrderByCreatedAtDesc(UUID caseId);
}
