package com.finance.adapters.in.rest.v1;

import com.finance.application.command.RegisterUserCommand;
import com.finance.application.command.UpdateUserCommand;
import com.finance.application.dto.RegisterUserDto;
import com.finance.application.dto.UpdateUserDto;
import com.finance.application.dto.UserResponseDto;
import com.finance.application.mapper.UserDtoMapper;
import com.finance.application.query.PageResult;
import com.finance.domain.model.User;
import com.finance.domain.ports.in.DeactivateUserUseCase;
import com.finance.domain.ports.in.GetUserByIdUseCase;
import com.finance.domain.ports.in.ListUsersUseCase;
import com.finance.domain.ports.in.RegisterUserUseCase;
import com.finance.domain.ports.in.UpdateUserUseCase;
import com.finance.shared.constants.AppConstants;
import com.finance.shared.response.ApiResponseDto;
import com.finance.shared.response.PageResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
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
@RequestMapping(AppConstants.API_V1 + "/users")
@Tag(name = "Usuários", description = "Cadastro e gestão de usuários do controle financeiro")
public class UserController {

    private final RegisterUserUseCase registerUserUseCase;
    private final UpdateUserUseCase updateUserUseCase;
    private final DeactivateUserUseCase deactivateUserUseCase;
    private final GetUserByIdUseCase getUserByIdUseCase;
    private final ListUsersUseCase listUsersUseCase;
    private final UserDtoMapper userDtoMapper;

    public UserController(RegisterUserUseCase registerUserUseCase, UpdateUserUseCase updateUserUseCase,
                          DeactivateUserUseCase deactivateUserUseCase, GetUserByIdUseCase getUserByIdUseCase,
                          ListUsersUseCase listUsersUseCase, UserDtoMapper userDtoMapper) {
        this.registerUserUseCase = registerUserUseCase;
        this.updateUserUseCase = updateUserUseCase;
        this.deactivateUserUseCase = deactivateUserUseCase;
        this.getUserByIdUseCase = getUserByIdUseCase;
        this.listUsersUseCase = listUsersUseCase;
        this.userDtoMapper = userDtoMapper;
    }

    @Operation(summary = "Cadastrar usuário (e-mail, CPF e senha com BCrypt)")
    @PostMapping
    public ResponseEntity<ApiResponseDto<UserResponseDto>> register(@Valid @RequestBody RegisterUserDto request) {
        User user = registerUserUseCase.handle(new RegisterUserCommand(request.email(), request.cpf(),
            request.password(), request.active()));
        UserResponseDto response = userDtoMapper.toResponseDto(user);
        return ResponseEntity.created(URI.create(AppConstants.API_V1 + "/users/" + user.getId()))
            .body(ApiResponseDto.of(response, AppConstants.MESSAGE_SUCCESS));
    }

    @Operation(summary = "Listar usuários (paginação e filtro por ativo)")
    @GetMapping
    public ResponseEntity<ApiResponseDto<PageResponseDto<UserResponseDto>>> list(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false) Boolean active) {
        PageResult<User> result = listUsersUseCase.handle(active, page, size);
        List<UserResponseDto> content = result.content().stream().map(userDtoMapper::toResponseDto).toList();
        return ResponseEntity.ok(ApiResponseDto.of(
            new PageResponseDto<>(content, result.page(), result.size(), result.totalElements(), result.totalPages()),
            AppConstants.MESSAGE_SUCCESS));
    }

    @Operation(summary = "Detalhar usuário")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDto<UserResponseDto>> getById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponseDto.of(userDtoMapper.toResponseDto(getUserByIdUseCase.handle(id)),
            AppConstants.MESSAGE_SUCCESS));
    }

    @Operation(summary = "Editar e-mail e CPF (somente o próprio usuário; auditoria sem senha)")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponseDto<UserResponseDto>> update(@PathVariable String id,
                                                                  @Valid @RequestBody UpdateUserDto request) {
        User user = updateUserUseCase.handle(id, new UpdateUserCommand(request.email(), request.cpf()));
        return ResponseEntity.ok(ApiResponseDto.of(userDtoMapper.toResponseDto(user), AppConstants.MESSAGE_SUCCESS));
    }

    @Operation(summary = "Desativar usuário (somente o próprio usuário; auditoria sem senha)")
    @PostMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponseDto<UserResponseDto>> deactivate(@PathVariable String id) {
        User user = deactivateUserUseCase.handle(id);
        return ResponseEntity.ok(ApiResponseDto.of(userDtoMapper.toResponseDto(user), AppConstants.MESSAGE_SUCCESS));
    }
}
