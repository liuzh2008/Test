package com.example.medaiassistant.dto;

public class PromptTemplateSimpleDTO {
    private int promptID;
    private String promptType;
    private String promptName;

    public PromptTemplateSimpleDTO() {
    }

    public PromptTemplateSimpleDTO(int promptID, String promptType, String promptName) {
        this.promptID = promptID;
        this.promptType = promptType;
        this.promptName = promptName;
    }

    // Getters and Setters
    public int getPromptID() {
        return promptID;
    }

    public void setPromptID(int promptID) {
        this.promptID = promptID;
    }

    public String getPromptType() {
        return promptType;
    }

    public void setPromptType(String promptType) {
        this.promptType = promptType;
    }

    public String getPromptName() {
        return promptName;
    }

    public void setPromptName(String promptName) {
        this.promptName = promptName;
    }
}
