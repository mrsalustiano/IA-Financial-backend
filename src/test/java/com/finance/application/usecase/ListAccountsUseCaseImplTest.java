package com.finance.application.usecase;

import com.finance.application.query.AccountSearchQuery;
import com.finance.application.query.PageResult;
import com.finance.domain.enums.AccountType;
import com.finance.domain.model.Account;
import com.finance.domain.ports.out.AccountRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListAccountsUseCaseImplTest {

    @Mock
    private AccountRepositoryPort accountRepositoryPort;

    @InjectMocks
    private ListAccountsUseCaseImpl useCase;

    @Test
    void delegatesSearchToRepository() {
        AccountSearchQuery query = new AccountSearchQuery(AccountType.CONTA_CORRENTE, true, PageRequest.of(0, 10));
        Account account = Account.create("acc-1", "LOCAL_USER", "Main", null, AccountType.CONTA_CORRENTE, true, Instant.now());
        PageResult<Account> expected = new PageResult<>(List.of(account), 0, 10, 1, 1);
        when(accountRepositoryPort.search(query)).thenReturn(expected);

        PageResult<Account> result = useCase.handle(query);

        assertThat(result).isSameAs(expected);
        assertThat(result.content()).hasSize(1);
        assertThat(result.totalElements()).isEqualTo(1);
    }
}
