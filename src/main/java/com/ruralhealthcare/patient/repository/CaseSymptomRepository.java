package com.ruralhealthcare.patient.repository;

import com.ruralhealthcare.patient.domain.CaseSymptom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CaseSymptomRepository extends JpaRepository<CaseSymptom, UUID> {

    List<CaseSymptom> findByHealthcareCaseId(UUID caseId);
}
