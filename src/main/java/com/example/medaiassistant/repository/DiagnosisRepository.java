package com.example.medaiassistant.repository;

import com.example.medaiassistant.model.Diagnosis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.repository.query.Param;

import java.util.List;

@Repository
public interface DiagnosisRepository extends JpaRepository<Diagnosis, Integer> {
@Query("SELECT d FROM Diagnosis d WHERE d.patientId = :patientId AND (d.isDeleted = 0 OR d.isDeleted IS NULL)")
List<Diagnosis> findByPatientId(@Param("patientId") String patientId);

@Query("SELECT d FROM Diagnosis d WHERE d.isDeleted = 0 OR d.isDeleted IS NULL")
List<Diagnosis> findAllActiveDiagnoses();

@Modifying
    @Transactional
    @Query("UPDATE Diagnosis d SET d.isDeleted = 1 WHERE d.diagnosisId = :diagnosisId")
    int softDeleteById(@Param("diagnosisId") Integer diagnosisId);
}
