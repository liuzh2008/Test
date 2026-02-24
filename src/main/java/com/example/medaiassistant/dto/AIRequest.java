package com.example.medaiassistant.dto;

import java.util.List;
import java.util.Map;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

public class AIRequest {
    @NotNull(message = "模型名称不能为空")
    private String model;
    
    @NotNull(message = "消息列表不能为空")
    @Size(min = 1, message = "至少需要一条消息")
    private List<Map<String, String>> messages;
    
    private boolean stream = false;
    
    // AI模型参数
    @DecimalMin(value = "0.0", message = "temperature不能小于0")
    @DecimalMax(value = "2.0", message = "temperature不能大于2")
    private Double temperature = 0.7;
    
    @Min(value = 1, message = "max_tokens不能小于1")
    @Max(value = 8192, message = "max_tokens不能大于8192")
    private Integer maxTokens = 8192;
    
    @DecimalMin(value = "0.0", message = "top_p不能小于0")
    @DecimalMax(value = "1.0", message = "top_p不能大于1")
    private Double topP = 0.9;
    
    @DecimalMin(value = "-2.0", message = "frequency_penalty不能小于-2")
    @DecimalMax(value = "2.0", message = "frequency_penalty不能大于2")
    private Double frequencyPenalty = 0.0;
    
    @DecimalMin(value = "-2.0", message = "presence_penalty不能小于-2")
    @DecimalMax(value = "2.0", message = "presence_penalty不能大于2")
    private Double presencePenalty = 0.0;
    
    @Min(value = 1, message = "n不能小于1")
    @Max(value = 10, message = "n不能大于10")
    private Integer n = 1;
    
    private Boolean logprobs = false;
    
    @Min(value = 0, message = "logprobs_top_logprobs不能小于0")
    @Max(value = 20, message = "logprobs_top_logprobs不能大于20")
    private Integer logprobsTopLogprobs = 0;
    
    private Boolean echo = false;
    
    private List<String> stop = null;
    
    private String user = null;

    // Getters and setters
    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public List<Map<String, String>> getMessages() {
        return messages;
    }

    public void setMessages(List<Map<String, String>> messages) {
        this.messages = messages;
    }

    public boolean isStream() {
        return stream;
    }

    public void setStream(boolean stream) {
        this.stream = stream;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public Integer getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
    }

    public Double getTopP() {
        return topP;
    }

    public void setTopP(Double topP) {
        this.topP = topP;
    }

    public Double getFrequencyPenalty() {
        return frequencyPenalty;
    }

    public void setFrequencyPenalty(Double frequencyPenalty) {
        this.frequencyPenalty = frequencyPenalty;
    }

    public Double getPresencePenalty() {
        return presencePenalty;
    }

    public void setPresencePenalty(Double presencePenalty) {
        this.presencePenalty = presencePenalty;
    }

    public Integer getN() {
        return n;
    }

    public void setN(Integer n) {
        this.n = n;
    }

    public Boolean getLogprobs() {
        return logprobs;
    }

    public void setLogprobs(Boolean logprobs) {
        this.logprobs = logprobs;
    }

    public Integer getLogprobsTopLogprobs() {
        return logprobsTopLogprobs;
    }

    public void setLogprobsTopLogprobs(Integer logprobsTopLogprobs) {
        this.logprobsTopLogprobs = logprobsTopLogprobs;
    }

    public Boolean getEcho() {
        return echo;
    }

    public void setEcho(Boolean echo) {
        this.echo = echo;
    }

    public List<String> getStop() {
        return stop;
    }

    public void setStop(List<String> stop) {
        this.stop = stop;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }
}
