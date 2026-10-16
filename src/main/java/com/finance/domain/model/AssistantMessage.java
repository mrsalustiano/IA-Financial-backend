package com.finance.domain.model;

import java.time.Instant;
import java.util.Objects;

public class AssistantMessage extends BaseEntity {

    private String question;
    private String answer;
    private String provider;
    private String model;

    private AssistantMessage(String id, String userId, String question, String answer, String provider,
                             String model, boolean deleted, Instant deletedAt, Instant createdAt,
                             Instant updatedAt) {
        super(id, userId, deleted, deletedAt, createdAt, updatedAt);
        this.question = question;
        this.answer = answer;
        this.provider = provider;
        this.model = model;
    }

    public static AssistantMessage create(String id, String userId, String question, String answer,
                                          String provider, String model, Instant now) {
        return new AssistantMessage(id, userId, question, answer, provider, model, false, null, now, now);
    }

    public static AssistantMessage rehydrate(String id, String userId, String question, String answer,
                                             String provider, String model, boolean deleted, Instant deletedAt,
                                             Instant createdAt, Instant updatedAt) {
        return new AssistantMessage(id, userId, question, answer, provider, model, deleted, deletedAt, createdAt,
            updatedAt);
    }

    public String getQuestion() {
        return question;
    }

    public String getAnswer() {
        return answer;
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
        if (!(o instanceof AssistantMessage message)) {
            return false;
        }
        return id != null && id.equals(message.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
