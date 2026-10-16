package com.finance.adapters.out.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BcryptPasswordHasherAdapterTest {

    private final BcryptPasswordHasherAdapter adapter = new BcryptPasswordHasherAdapter();

    @Test
    void hashesWithBcryptAndMatches() {
        String hash = adapter.hash("Senha@123");

        assertThat(hash).isNotEqualTo("Senha@123").startsWith("$2");
        assertThat(adapter.matches("Senha@123", hash)).isTrue();
        assertThat(adapter.matches("errada", hash)).isFalse();
    }
}
