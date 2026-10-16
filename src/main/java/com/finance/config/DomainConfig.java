package com.finance.config;

import com.finance.domain.ports.out.AccountRepositoryPort;
import com.finance.domain.services.AccountDomainService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainConfig {

    @Bean
    public AccountDomainService accountDomainService(AccountRepositoryPort accountRepositoryPort) {
        return new AccountDomainService(accountRepositoryPort);
    }
}
