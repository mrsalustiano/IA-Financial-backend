package com.finance.adapters.out.security;

import com.finance.domain.ports.out.CurrentUserPort;
import com.finance.shared.exception.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class SecurityContextCurrentUserAdapter implements CurrentUserPort {

    @Override
    public String requireUserId() {
        return currentUserId().orElseThrow(() -> new UnauthorizedException("Usuário não autenticado"));
    }

    @Override
    public Optional<String> currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal() == null) {
            return Optional.empty();
        }
        return Optional.of(String.valueOf(authentication.getPrincipal()));
    }
}
