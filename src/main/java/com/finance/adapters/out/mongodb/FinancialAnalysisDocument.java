package com.finance.adapters.out.mongodb;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Document(collection = "financial_analyses")
public class FinancialAnalysisDocument {

    @Id
    private String id;
    private String userId;
    private String prompt;
    private String response;
    private long score;
    private List<String> recommendations;
    private String provider;
    private String model;
    private boolean deleted;
    private Instant deletedAt;
    private Instant createdAt;
    private Instant updatedAt;

    public FinancialAnalysisDocument() {
    }

    public FinancialAnalysisDocument(String id, String userId, String prompt, String response, long score,
                                     List<String> recommendations, String provider, String model, boolean deleted,
                                     Instant deletedAt, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.userId = userId;
        this.prompt = prompt;
        this.response = response;
        this.score = score;
        this.recommendations = recommendations;
        this.provider = provider;
        this.model = model;
        this.deleted = deleted;
        this.deletedAt = deletedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public long getScore() {
        return score;
    }

    public void setScore(long score) {
        this.score = score;
    }

    public List<String> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(List<String> recommendations) {
        this.recommendations = recommendations;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
