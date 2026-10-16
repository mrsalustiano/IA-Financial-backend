package com.finance.adapters.in.rest.v1;

import com.finance.application.dto.CreateMonthlyClosingDto;
import com.finance.application.dto.MonthlyClosingResponseDto;

import com.finance.domain.ports.in.GenerateMonthlyClosingUseCase;
import com.finance.domain.ports.in.GetMonthlyClosingByIdUseCase;
import com.finance.domain.ports.in.ListMonthlyClosingsUseCase;
import com.finance.domain.model.MonthlyClosing;
import com.finance.shared.constants.AppConstants;
import com.finance.shared.response.ApiResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping(AppConstants.API_V1 + "/monthly-closings")
@Tag(name = "Fechamento Mensal", description = "Snapshots mensais do saldo (scheduler e geração manual)")
public class MonthlyClosingController {

    private final GenerateMonthlyClosingUseCase generateMonthlyClosingUseCase;
    private final GetMonthlyClosingByIdUseCase getMonthlyClosingByIdUseCase;
    private final ListMonthlyClosingsUseCase listMonthlyClosingsUseCase;

    public MonthlyClosingController(GenerateMonthlyClosingUseCase generateMonthlyClosingUseCase,
                                    GetMonthlyClosingByIdUseCase getMonthlyClosingByIdUseCase,
                                    ListMonthlyClosingsUseCase listMonthlyClosingsUseCase) {
        this.generateMonthlyClosingUseCase = generateMonthlyClosingUseCase;
        this.getMonthlyClosingByIdUseCase = getMonthlyClosingByIdUseCase;
        this.listMonthlyClosingsUseCase = listMonthlyClosingsUseCase;
    }

    @Operation(summary = "Gerar fechamento mensal (snapshot persistido com auditoria)")
    @PostMapping
    public ResponseEntity<ApiResponseDto<MonthlyClosingResponseDto>> generate(
        @Valid @RequestBody CreateMonthlyClosingDto request) {
        MonthlyClosing closing = generateMonthlyClosingUseCase.handle(request.month());
        return ResponseEntity
            .created(URI.create(AppConstants.API_V1 + "/monthly-closings/" + closing.getId()))
            .body(ApiResponseDto.of(toResponseDto(closing), AppConstants.MESSAGE_SUCCESS));
    }

    @Operation(summary = "Buscar fechamento mensal por id")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDto<MonthlyClosingResponseDto>> findById(@PathVariable String id) {
        MonthlyClosing closing = getMonthlyClosingByIdUseCase.handle(id);
        return ResponseEntity.ok(ApiResponseDto.of(toResponseDto(closing), AppConstants.MESSAGE_SUCCESS));
    }

    @Operation(summary = "Listar fechamentos mensais")
    @GetMapping
    public ResponseEntity<ApiResponseDto<List<MonthlyClosingResponseDto>>> list() {
        List<MonthlyClosingResponseDto> closings = listMonthlyClosingsUseCase.handle().stream()
            .map(MonthlyClosingController::toResponseDto)
            .toList();
        return ResponseEntity.ok(ApiResponseDto.of(closings, AppConstants.MESSAGE_SUCCESS));
    }

    private static MonthlyClosingResponseDto toResponseDto(MonthlyClosing closing) {
        return new MonthlyClosingResponseDto(closing.getId(), closing.getUserId(), closing.getReferenceMonth(),
            closing.getTotalIncome(), closing.getTotalExpense(), closing.getBalance(), closing.getClosedAt(),
            closing.getCreatedAt());
    }
}
