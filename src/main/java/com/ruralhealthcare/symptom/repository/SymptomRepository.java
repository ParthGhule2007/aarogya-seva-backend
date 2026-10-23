package com.ruralhealthcare.symptom.repository;

import com.ruralhealthcare.symptom.domain.Symptom;
import com.ruralhealthcare.symptom.domain.SymptomSeverity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface SymptomRepository extends JpaRepository<Symptom, UUID> {

    List<Symptom> findByCaseIdOrderByCreatedAtDesc(UUID caseId);

    List<Symptom> findByCaseIdAndSeverity(UUID caseId, SymptomSeverity severity);

    boolean existsByCaseId(UUID caseId);

    long countByCaseId(UUID caseId);

    long countByCaseIdAndSeverityIn(UUID caseId, Collection<SymptomSeverity> severities);

    void deleteByCaseId(UUID caseId);
}
