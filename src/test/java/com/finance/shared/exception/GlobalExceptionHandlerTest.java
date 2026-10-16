package com.finance.shared.exception;

import com.finance.shared.response.ErrorResponseDto;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void mapsValidationExceptionTo400() {
        ResponseEntity<ErrorResponseDto> response = handler.handleValidation(new ValidationException("bad input"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().status()).isEqualTo(400);
        assertThat(response.getBody().error()).isEqualTo("BAD_REQUEST");
        assertThat(response.getBody().message()).isEqualTo("bad input");
        assertThat(response.getBody().timestamp()).isNotNull();
    }

    @Test
    void mapsNotFoundExceptionTo404() {
        ResponseEntity<ErrorResponseDto> response = handler.handleNotFound(new NotFoundException("missing"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().error()).isEqualTo("NOT_FOUND");
        assertThat(response.getBody().message()).isEqualTo("missing");
    }

    @Test
    void mapsConflictExceptionTo409() {
        ResponseEntity<ErrorResponseDto> response = handler.handleConflict(new ConflictException("duplicate"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().error()).isEqualTo("CONFLICT");
    }

    @Test
    void mapsUnauthorizedExceptionTo401() {
        ResponseEntity<ErrorResponseDto> response = handler.handleUnauthorized(new UnauthorizedException("no token"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().error()).isEqualTo("UNAUTHORIZED");
    }

    @Test
    void mapsForbiddenExceptionTo403() {
        ResponseEntity<ErrorResponseDto> response = handler.handleForbidden(new ForbiddenException("denied"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().error()).isEqualTo("FORBIDDEN");
    }

    @Test
    void mapsBusinessExceptionTo422() {
        ResponseEntity<ErrorResponseDto> response = handler.handleBusiness(new BusinessException("rule broken"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody().error()).isEqualTo("UNPROCESSABLE_ENTITY");
        assertThat(response.getBody().message()).isEqualTo("rule broken");
        assertThat(response.getBody().details()).isEqualTo(List.of());
    }

    @Test
    void mapsDatabaseExceptionTo500() {
        ResponseEntity<ErrorResponseDto> response =
            handler.handleDatabase(new DatabaseException("db down", new RuntimeException()));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().error()).isEqualTo("INTERNAL_SERVER_ERROR");
        assertThat(response.getBody().message()).isEqualTo("db down");
    }

    @Test
    void mapsIntegrationAndExternalServiceExceptionsTo503() {
        ResponseEntity<ErrorResponseDto> integration =
            handler.handleIntegration(new IntegrationException("offline"));
        ResponseEntity<ErrorResponseDto> external =
            handler.handleIntegration(new ExternalServiceException("provider error"));

        assertThat(integration.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(integration.getBody().error()).isEqualTo("SERVICE_UNAVAILABLE");
        assertThat(external.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }
}
