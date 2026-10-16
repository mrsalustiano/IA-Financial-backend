package com.finance.adapters.in.rest.v1;

import com.finance.application.dto.DailyBalanceResponseDto;
import com.finance.domain.model.DailyBalanceEntry;
import com.finance.domain.ports.in.GetDailyBalanceUseCase;
import com.finance.shared.constants.AppConstants;
import com.finance.shared.response.ApiResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping(AppConstants.API_V1 + "/daily-balance")
@Tag(name = "Saldo Diário", description = "Saldo diário acumulado das movimentações")
public class DailyBalanceController {

    private final GetDailyBalanceUseCase getDailyBalanceUseCase;

    public DailyBalanceController(GetDailyBalanceUseCase getDailyBalanceUseCase) {
        this.getDailyBalanceUseCase = getDailyBalanceUseCase;
    }

    @Operation(summary = "Consultar saldo diário acumulado")
    @GetMapping
    public ResponseEntity<ApiResponseDto<List<DailyBalanceResponseDto>>> findDailyBalance(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<DailyBalanceResponseDto> balances = getDailyBalanceUseCase.handle(startDate, endDate).stream()
            .map(DailyBalanceController::toResponseDto)
            .toList();
        return ResponseEntity.ok(ApiResponseDto.of(balances, AppConstants.MESSAGE_SUCCESS));
    }

    private static DailyBalanceResponseDto toResponseDto(DailyBalanceEntry entry) {
        return new DailyBalanceResponseDto(entry.date(), entry.totalIncome(), entry.totalExpense(),
            entry.balance(), entry.accumulated());
    }
}
