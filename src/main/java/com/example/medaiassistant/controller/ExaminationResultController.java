package com.example.medaiassistant.controller;

import com.example.medaiassistant.model.ExaminationResult;
import com.example.medaiassistant.repository.ExaminationResultRepository;
import com.example.medaiassistant.service.ExaminationResultService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/examination-results")
public class ExaminationResultController {

    @Autowired
    private ExaminationResultRepository examinationResultRepository;

    @Autowired
    private ExaminationResultService examinationResultService;

    @GetMapping("/by-patient/{patientId}")
    public List<ExaminationResult> getExaminationResultsByPatientId(@PathVariable String patientId) {
        return examinationResultRepository.findByPatientId(patientId);
    }

    @GetMapping("/formatted/by-patient/{patientId}")
    public String getFormattedExaminationResultsByPatientId(@PathVariable String patientId) {
        return examinationResultService.getFormattedResultsByPatientId(patientId);
    }
}
