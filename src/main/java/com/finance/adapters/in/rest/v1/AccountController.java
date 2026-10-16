package com.finance.adapters.in.rest.v1;

import com.finance.application.dto.AccountResponseDto;
import com.finance.application.dto.CreateAccountDto;
import com.finance.application.dto.UpdateAccountDto;
import com.finance.application.mapper.AccountDtoMapper;
import com.finance.application.query.AccountSearchQuery;
import com.finance.application.query.PageResult;
import com.finance.domain.enums.AccountType;
import com.finance.domain.model.Account;
import com.finance.domain.ports.in.CreateAccountUseCase;
import com.finance.domain.ports.in.DeleteAccountUseCase;
import com.finance.domain.ports.in.GetAccountByIdUseCase;
import com.finance.domain.ports.in.ListAccountsUseCase;
import com.finance.domain.ports.in.UpdateAccountUseCase;
import com.finance.shared.constants.AppConstants;
import com.finance.shared.response.ApiResponseDto;
import com.finance.shared.response.PageResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping(AppConstants.API_V1 + "/accounts")
@Tag(name = "Contas", description = "Gerenciamento de contas financeiras")
public class AccountController {

    private final CreateAccountUseCase createAccountUseCase;
    private final UpdateAccountUseCase updateAccountUseCase;
    private final DeleteAccountUseCase deleteAccountUseCase;
    private final GetAccountByIdUseCase getAccountByIdUseCase;
    private final ListAccountsUseCase listAccountsUseCase;
    private final AccountDtoMapper accountDtoMapper;

    public AccountController(CreateAccountUseCase createAccountUseCase, UpdateAccountUseCase updateAccountUseCase,
                             DeleteAccountUseCase deleteAccountUseCase, GetAccountByIdUseCase getAccountByIdUseCase,
                             ListAccountsUseCase listAccountsUseCase, AccountDtoMapper accountDtoMapper) {
        this.createAccountUseCase = createAccountUseCase;
        this.updateAccountUseCase = updateAccountUseCase;
        this.deleteAccountUseCase = deleteAccountUseCase;
        this.getAccountByIdUseCase = getAccountByIdUseCase;
        this.listAccountsUseCase = listAccountsUseCase;
        this.accountDtoMapper = accountDtoMapper;
    }

    @Operation(summary = "Criar uma nova conta")
    @PostMapping
    public ResponseEntity<ApiResponseDto<AccountResponseDto>> create(@Valid @RequestBody CreateAccountDto dto) {
        Account account = createAccountUseCase.handle(accountDtoMapper.toCommand(dto));
        AccountResponseDto response = accountDtoMapper.toResponseDto(account);
        return ResponseEntity.created(URI.create(AppConstants.API_V1 + "/accounts/" + account.getId()))
            .body(ApiResponseDto.of(response, AppConstants.MESSAGE_SUCCESS));
    }

    @Operation(summary = "Atualizar uma conta existente")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponseDto<AccountResponseDto>> update(@PathVariable String id,
                                                                     @Valid @RequestBody UpdateAccountDto dto) {
        Account account = updateAccountUseCase.handle(id, accountDtoMapper.toCommand(dto));
        return ResponseEntity.ok(ApiResponseDto.of(accountDtoMapper.toResponseDto(account),
            AppConstants.MESSAGE_SUCCESS));
    }

    @Operation(summary = "Excluir uma conta (exclusão lógica)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        deleteAccountUseCase.handle(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Buscar conta por id")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDto<AccountResponseDto>> findById(@PathVariable String id) {
        Account account = getAccountByIdUseCase.handle(id);
        return ResponseEntity.ok(ApiResponseDto.of(accountDtoMapper.toResponseDto(account),
            AppConstants.MESSAGE_SUCCESS));
    }

    @Operation(summary = "Listar contas com filtros, paginação e ordenação")
    @GetMapping
    public ResponseEntity<ApiResponseDto<PageResponseDto<AccountResponseDto>>> list(
        @RequestParam(required = false) AccountType type,
        @RequestParam(required = false) Boolean active,
        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResult<Account> result = listAccountsUseCase.handle(new AccountSearchQuery(type, active, pageable));
        List<AccountResponseDto> content = result.content().stream().map(accountDtoMapper::toResponseDto).toList();
        PageResponseDto<AccountResponseDto> page = new PageResponseDto<>(content, result.page(), result.size(),
            result.totalElements(), result.totalPages());
        return ResponseEntity.ok(ApiResponseDto.of(page, AppConstants.MESSAGE_SUCCESS));
    }
}
