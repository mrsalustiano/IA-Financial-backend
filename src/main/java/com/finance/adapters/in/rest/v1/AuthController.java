package com.finance.adapters.in.rest.v1;

import com.finance.application.dto.AuthResponseDto;
import com.finance.application.dto.LoginDto;
import com.finance.application.dto.RefreshTokenRequestDto;
import com.finance.application.dto.UserResponseDto;
import com.finance.application.mapper.UserDtoMapper;
import com.finance.domain.model.AuthSession;
import com.finance.domain.model.User;
import com.finance.domain.ports.in.AuthenticateUseCase;
import com.finance.domain.ports.in.GetCurrentUserUseCase;
import com.finance.domain.ports.in.LogoutUseCase;
import com.finance.domain.ports.in.RefreshAccessTokenUseCase;
import com.finance.shared.constants.AppConstants;
import com.finance.shared.response.ApiResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(AppConstants.API_V1 + "/auth")
@Tag(name = "Autenticação", description = "Login, renovação e encerramento de sessão com JWT")
public class AuthController {

    private final AuthenticateUseCase authenticateUseCase;
    private final RefreshAccessTokenUseCase refreshAccessTokenUseCase;
    private final LogoutUseCase logoutUseCase;
    private final GetCurrentUserUseCase getCurrentUserUseCase;
    private final UserDtoMapper userDtoMapper;

    public AuthController(AuthenticateUseCase authenticateUseCase,
                          RefreshAccessTokenUseCase refreshAccessTokenUseCase, LogoutUseCase logoutUseCase,
                          GetCurrentUserUseCase getCurrentUserUseCase, UserDtoMapper userDtoMapper) {
        this.authenticateUseCase = authenticateUseCase;
        this.refreshAccessTokenUseCase = refreshAccessTokenUseCase;
        this.logoutUseCase = logoutUseCase;
        this.getCurrentUserUseCase = getCurrentUserUseCase;
        this.userDtoMapper = userDtoMapper;
    }

    @Operation(summary = "Autenticar e emitir tokens de acesso/renovação")
    @PostMapping("/login")
    public ResponseEntity<ApiResponseDto<AuthResponseDto>> login(@Valid @RequestBody LoginDto request) {
        AuthSession session = authenticateUseCase.handle(request.email(), request.password());
        return ResponseEntity.ok(ApiResponseDto.of(userDtoMapper.toResponseDto(session),
            AppConstants.MESSAGE_SUCCESS));
    }

    @Operation(summary = "Renovar tokens a partir do refresh token (com rotação)")
    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponseDto<AuthResponseDto>> refreshToken(
        @Valid @RequestBody RefreshTokenRequestDto request) {
        AuthSession session = refreshAccessTokenUseCase.handle(request.refreshToken());
        return ResponseEntity.ok(ApiResponseDto.of(userDtoMapper.toResponseDto(session),
            AppConstants.MESSAGE_SUCCESS));
    }

    @Operation(summary = "Encerrar sessão revogando o refresh token")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequestDto request) {
        logoutUseCase.handle(request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Retornar o usuário autenticado no token")
    @GetMapping("/me")
    public ResponseEntity<ApiResponseDto<UserResponseDto>> me() {
        User user = getCurrentUserUseCase.handle();
        return ResponseEntity.ok(ApiResponseDto.of(userDtoMapper.toResponseDto(user),
            AppConstants.MESSAGE_SUCCESS));
    }
}
