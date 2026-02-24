package com.example.medaiassistant.dto;

import com.example.medaiassistant.model.ConversationHistory;
import lombok.Data;
import java.util.Date;

@Data
public class ConversationHistoryDTO {
    private String sessionId;
    private String userId;
    private String patientId;
    private ConversationHistory.MessageType messageType;
    private String content;
    private String modelName;
    private Date timestamp;
}
