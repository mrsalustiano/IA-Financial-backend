package com.finance.adapters.in.rest.v1;

import com.finance.application.dto.CreateFinancialPlanDto;
import com.finance.application.dto.FinancialPlanResponseDto;
import com.finance.application.mapper.FinancialPlanDtoMapper;
import com.finance.application.query.PageResult;
import com.finance.domain.model.FinancialPlan;
import com.finance.domain.ports.in.CreateFinancialPlanUseCase;
import com.finance.domain.ports.in.GetFinancialPlanByIdUseCase;
import com.finance.domain.ports.in.ListFinancialPlansUseCase;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping(AppConstants.API_V1 + "/financial-plans")
@Tag(name = "Planejamento Financeiro", description = "Planos de economia sugeridos pela IA")
public class FinancialPlanController {

    private final CreateFinancialPlanUseCase createFinancialPlanUseCase;
    private final GetFinancialPlanByIdUseCase getFinancialPlanByIdUseCase;
    private final ListFinancialPlansUseCase listFinancialPlansUseCase;
    private final FinancialPlanDtoMapper planDtoMapper;

    public FinancialPlanController(CreateFinancialPlanUseCase createFinancialPlanUseCase,
                                   GetFinancialPlanByIdUseCase getFinancialPlanByIdUseCase,
                                   ListFinancialPlansUseCase listFinancialPlansUseCase,
                                   FinancialPlanDtoMapper planDtoMapper) {
        this.createFinancialPlanUseCase = createFinancialPlanUseCase;
        this.getFinancialPlanByIdUseCase = getFinancialPlanByIdUseCase;
        this.listFinancialPlansUseCase = listFinancialPlansUseCase;
        this.planDtoMapper = planDtoMapper;
    }

    @Operation(summary = "Criar plano de economia com IA a partir de uma meta")
    @PostMapping
    public ResponseEntity<ApiResponseDto<FinancialPlanResponseDto>> create(
        @Valid @RequestBody CreateFinancialPlanDto request) {
        FinancialPlan plan = createFinancialPlanUseCase.handle(planDtoMapper.toCommand(request));
        return ResponseEntity.created(URI.create(AppConstants.API_V1 + "/financial-plans/" + plan.getId()))
            .body(ApiResponseDto.of(planDtoMapper.toResponseDto(plan), AppConstants.MESSAGE_SUCCESS));
    }

    @Operation(summary = "Buscar plano por id")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDto<FinancialPlanResponseDto>> findById(@PathVariable String id) {
        FinancialPlan plan = getFinancialPlanByIdUseCase.handle(id);
        return ResponseEntity.ok(ApiResponseDto.of(planDtoMapper.toResponseDto(plan), AppConstants.MESSAGE_SUCCESS));
    }

    @Operation(summary = "Listar histórico de planos com paginação")
    @GetMapping
    public ResponseEntity<ApiResponseDto<PageResponseDto<FinancialPlanResponseDto>>> list(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {
        PageResult<FinancialPlan> result = listFinancialPlansUseCase.handle(page, size);
        List<FinancialPlanResponseDto> content = result.content().stream().map(planDtoMapper::toResponseDto).toList();
        return ResponseEntity.ok(ApiResponseDto.of(new PageResponseDto<>(content, result.page(), result.size(),
            result.totalElements(), result.totalPages()), AppConstants.MESSAGE_SUCCESS));
    }
}
