package com.example.medaiassistant.dto;

import lombok.Data;
import java.util.Date;

@Data
public class PatientBasicInfoDTO {
    private String gender;
    private Integer age;
    private Date admissionTime;
    private Long hospitalizationDays;
}
