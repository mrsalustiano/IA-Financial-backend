package com.finance.shared.response;

import java.time.Instant;

public record ApiResponseDto<T>(T data, String message, Instant timestamp) {

    public static <T> ApiResponseDto<T> of(T data, String message) {
        return new ApiResponseDto<>(data, message, Instant.now());
    }
}
