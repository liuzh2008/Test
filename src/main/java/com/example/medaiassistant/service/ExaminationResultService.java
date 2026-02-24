package com.example.medaiassistant.service;

import com.example.medaiassistant.model.ExaminationResult;
import com.example.medaiassistant.repository.ExaminationResultRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ExaminationResultService {

    @Autowired
    private ExaminationResultRepository examinationResultRepository;
    
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public String getFormattedResultsByPatientId(String patientId) {
        List<ExaminationResult> results = examinationResultRepository.findByPatientId(patientId);
        
        return results.stream()
            .map(result -> String.format(
                "%s %s\n描述：\n%s\n结论：\n%s\n",
                result.getCheckReportTime() != null ? DATE_FORMAT.format(result.getCheckReportTime()) : "",
                result.getCheckName(),
                result.getCheckDescription(),
                result.getCheckConclusion()
            ))
            .collect(Collectors.joining("\n"));
    }
}
