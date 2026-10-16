package com.finance.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class JwtPropertiesTest {

    @Test
    void exposesConfiguredValuesAndDefaults() {
        JwtProperties properties = new JwtProperties();
        assertThat(properties.getIssuer()).isEqualTo("ia-financial");
        assertThat(properties.getAccessTokenTtl()).isEqualTo(Duration.ofMinutes(15));
        assertThat(properties.getRefreshTokenTtl()).isEqualTo(Duration.ofDays(7));

        properties.setSecret("segredo");
        properties.setIssuer("outro");
        properties.setAccessTokenTtl(Duration.ofMinutes(30));
        properties.setRefreshTokenTtl(Duration.ofDays(1));
        assertThat(properties.getSecret()).isEqualTo("segredo");
        assertThat(properties.getIssuer()).isEqualTo("outro");
        assertThat(properties.getAccessTokenTtl()).isEqualTo(Duration.ofMinutes(30));
        assertThat(properties.getRefreshTokenTtl()).isEqualTo(Duration.ofDays(1));
    }
}
