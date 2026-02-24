package com.example.medaiassistant.model;

import jakarta.persistence.*;
import lombok.Data;
import java.util.Date;

@Data
@Entity
@Table(name = "conversation_history")
public class ConversationHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", length = 64)
    private String sessionId;

    @Column(name = "user_id", length = 64)
    private String userId;

    @Column(name = "patient_id", length = 64)
    private String patientId;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type")
    private MessageType messageType;

    @Column(columnDefinition = "text")
    private String content;

    @Column(name = "model_name", length = 64)
    private String modelName;

    @Column(name = "timestamp")
    private Date timestamp;

    public enum MessageType {
        user, ai
    }
}
