package com.finance.shared.response;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ResponseDtosTest {

    @Test
    void apiResponseDtoFactorySetsTimestampAndMessage() {
        ApiResponseDto<String> response = ApiResponseDto.of("payload", "Success");

        assertThat(response.data()).isEqualTo("payload");
        assertThat(response.message()).isEqualTo("Success");
        assertThat(response.timestamp()).isBeforeOrEqualTo(Instant.now());
    }

    @Test
    void pageResponseDtoHoldsPaginationMetadata() {
        PageResponseDto<String> page = new PageResponseDto<>(List.of("a"), 2, 10, 21, 3);

        assertThat(page.content()).containsExactly("a");
        assertThat(page.page()).isEqualTo(2);
        assertThat(page.size()).isEqualTo(10);
        assertThat(page.totalElements()).isEqualTo(21);
        assertThat(page.totalPages()).isEqualTo(3);
    }
}
