package com.finance.domain.model;

import java.util.Map;

public record AiPrompt(String prompt, Map<String, Object> context) {

    public AiPrompt {
        context = context == null ? Map.of() : Map.copyOf(context);
    }
}
