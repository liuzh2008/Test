package com.example.medaiassistant.repository;

import com.example.medaiassistant.model.MedicalRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, Integer> {
    List<MedicalRecord> findByPatientIdAndDeleted(String patientId, Integer deleted);
    
    List<MedicalRecord> findByPatientIdAndDeletedOrderByRecordTimeDesc(String patientId, Integer deleted);

    @Modifying
    @Query("UPDATE MedicalRecord mr SET mr.deleted = 1 WHERE mr.recordId = :recordId")
    int softDeleteByRecordId(@Param("recordId") int recordId);
}
