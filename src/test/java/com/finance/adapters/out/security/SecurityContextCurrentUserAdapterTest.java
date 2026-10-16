package com.finance.adapters.out.security;

import com.finance.shared.exception.UnauthorizedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecurityContextCurrentUserAdapterTest {

    private final SecurityContextCurrentUserAdapter adapter = new SecurityContextCurrentUserAdapter();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsUserIdFromSecurityContext() {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("u-1", null, List.of()));

        assertThat(adapter.requireUserId()).isEqualTo("u-1");
        assertThat(adapter.currentUserId()).isEqualTo(Optional.of("u-1"));
    }

    @Test
    void throwsWhenUnauthenticated() {
        SecurityContextHolder.clearContext();

        assertThat(adapter.currentUserId()).isEmpty();
        assertThatThrownBy(() -> adapter.requireUserId()).isInstanceOf(UnauthorizedException.class)
            .hasMessageContaining("não autenticado");
    }
}
