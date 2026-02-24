package com.example.medaiassistant.repository;

import com.example.medaiassistant.model.Prompt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PromptRepository extends JpaRepository<Prompt, Integer> {
    List<Prompt> findByPatientIdAndPromptTemplateName(String patientId, String promptTemplateName);
    
    List<Prompt> findByPatientId(String patientId);
    
    List<Prompt> findByStatusName(String statusName);
    Page<Prompt> findByStatusName(String statusName, Pageable pageable);
    
    @Query("SELECT p FROM Prompt p WHERE p.statusName = '待处理' ORDER BY p.priority, p.submissionTime")
    Page<Prompt> findPendingPrompts(Pageable pageable);
    
    long countByStatusName(String statusName);

    long countByPatientIdAndPromptTemplateNameAndSubmissionTimeAfter(String patientId, String promptTemplateName, java.time.LocalDateTime submissionTimeAfter);

    Optional<Prompt> findTopByPatientIdAndPromptTemplateNameOrderBySubmissionTimeDesc(String patientId, String promptTemplateName);
}
