package com.finance.adapters.out.ollama;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OllamaRequest(String model, String prompt, boolean stream) {

    public static OllamaRequest of(String model, String prompt) {
        return new OllamaRequest(model, prompt, false);
    }
}
