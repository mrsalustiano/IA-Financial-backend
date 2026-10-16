package com.finance.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public class FinancialPlan extends BaseEntity {

    private String goal;
    private BigDecimal targetAmount;
    private LocalDate targetDate;
    private String prompt;
    private String response;
    private List<String> recommendations;
    private String provider;
    private String model;

    private FinancialPlan(String id, String userId, String goal, BigDecimal targetAmount, LocalDate targetDate,
                          String prompt, String response, List<String> recommendations, String provider,
                          String model, boolean deleted, Instant deletedAt, Instant createdAt, Instant updatedAt) {
        super(id, userId, deleted, deletedAt, createdAt, updatedAt);
        this.goal = goal;
        this.targetAmount = targetAmount;
        this.targetDate = targetDate;
        this.prompt = prompt;
        this.response = response;
        this.recommendations = recommendations;
        this.provider = provider;
        this.model = model;
    }

    public static FinancialPlan create(String id, String userId, String goal, BigDecimal targetAmount,
                                       LocalDate targetDate, String prompt, String response,
                                       List<String> recommendations, String provider, String model, Instant now) {
        return new FinancialPlan(id, userId, goal, targetAmount, targetDate, prompt, response,
            List.copyOf(recommendations), provider, model, false, null, now, now);
    }

    public static FinancialPlan rehydrate(String id, String userId, String goal, BigDecimal targetAmount,
                                          LocalDate targetDate, String prompt, String response,
                                          List<String> recommendations, String provider, String model,
                                          boolean deleted, Instant deletedAt, Instant createdAt,
                                          Instant updatedAt) {
        return new FinancialPlan(id, userId, goal, targetAmount, targetDate, prompt, response, recommendations,
            provider, model, deleted, deletedAt, createdAt, updatedAt);
    }

    public String getGoal() {
        return goal;
    }

    public BigDecimal getTargetAmount() {
        return targetAmount;
    }

    public LocalDate getTargetDate() {
        return targetDate;
    }

    public String getPrompt() {
        return prompt;
    }

    public String getResponse() {
        return response;
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
        if (!(o instanceof FinancialPlan plan)) {
            return false;
        }
        return id != null && id.equals(plan.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
