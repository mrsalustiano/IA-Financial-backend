package com.finance.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public class FinancialAnalysis extends BaseEntity {

    private String prompt;
    private String response;
    private long score;
    private List<String> recommendations;
    private String provider;
    private String model;

    private FinancialAnalysis(String id, String userId, String prompt, String response, long score,
                              List<String> recommendations, String provider, String model, boolean deleted,
                              Instant deletedAt, Instant createdAt, Instant updatedAt) {
        super(id, userId, deleted, deletedAt, createdAt, updatedAt);
        this.prompt = prompt;
        this.response = response;
        this.score = score;
        this.recommendations = recommendations;
        this.provider = provider;
        this.model = model;
    }

    public static FinancialAnalysis create(String id, String userId, String prompt, String response, long score,
                                           List<String> recommendations, String provider, String model,
                                           Instant now) {
        return new FinancialAnalysis(id, userId, prompt, response, score, List.copyOf(recommendations), provider,
            model, false, null, now, now);
    }

    public static FinancialAnalysis rehydrate(String id, String userId, String prompt, String response, long score,
                                              List<String> recommendations, String provider, String model,
                                              boolean deleted, Instant deletedAt, Instant createdAt,
                                              Instant updatedAt) {
        return new FinancialAnalysis(id, userId, prompt, response, score, recommendations, provider, model, deleted,
            deletedAt, createdAt, updatedAt);
    }

    public String getPrompt() {
        return prompt;
    }

    public String getResponse() {
        return response;
    }

    public long getScore() {
        return score;
    }

    public List<String> getRecommendations() {
        return recommendations;
    }

    public String getProvider() {
        return provider;
    }

    public String getModel() {
        return model;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FinancialAnalysis analysis)) {
            return false;
        }
        return id != null && id.equals(analysis.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
