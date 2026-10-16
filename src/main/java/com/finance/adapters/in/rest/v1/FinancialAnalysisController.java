package com.finance.adapters.in.rest.v1;

import com.finance.application.dto.CreateFinancialAnalysisDto;
import com.finance.application.dto.FinancialAnalysisResponseDto;
import com.finance.application.mapper.FinancialAnalysisDtoMapper;
import com.finance.application.query.PageResult;
import com.finance.domain.model.FinancialAnalysis;
import com.finance.domain.ports.in.GenerateFinancialAnalysisUseCase;
import com.finance.domain.ports.in.GetFinancialAnalysisByIdUseCase;
import com.finance.domain.ports.in.ListFinancialAnalysesUseCase;
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
@RequestMapping(AppConstants.API_V1 + "/financial-analysis")
@Tag(name = "Análises Financeiras", description = "Análises financeiras geradas com IA (Ollama)")
public class FinancialAnalysisController {

    private final GenerateFinancialAnalysisUseCase generateFinancialAnalysisUseCase;
    private final GetFinancialAnalysisByIdUseCase getFinancialAnalysisByIdUseCase;
    private final ListFinancialAnalysesUseCase listFinancialAnalysesUseCase;
    private final FinancialAnalysisDtoMapper analysisDtoMapper;

    public FinancialAnalysisController(GenerateFinancialAnalysisUseCase generateFinancialAnalysisUseCase,
                                       GetFinancialAnalysisByIdUseCase getFinancialAnalysisByIdUseCase,
                                       ListFinancialAnalysesUseCase listFinancialAnalysesUseCase,
                                       FinancialAnalysisDtoMapper analysisDtoMapper) {
        this.generateFinancialAnalysisUseCase = generateFinancialAnalysisUseCase;
        this.getFinancialAnalysisByIdUseCase = getFinancialAnalysisByIdUseCase;
        this.listFinancialAnalysesUseCase = listFinancialAnalysesUseCase;
        this.analysisDtoMapper = analysisDtoMapper;
    }

    @Operation(summary = "Gerar uma nova análise financeira com IA")
    @PostMapping
    public ResponseEntity<ApiResponseDto<FinancialAnalysisResponseDto>> generate(
        @Valid @RequestBody(required = false) CreateFinancialAnalysisDto request) {
        CreateFinancialAnalysisDto dto = request == null ? new CreateFinancialAnalysisDto(null) : request;
        FinancialAnalysis analysis = generateFinancialAnalysisUseCase.handle(analysisDtoMapper.toCommand(dto));
        FinancialAnalysisResponseDto response = analysisDtoMapper.toResponseDto(analysis);
        return ResponseEntity.created(URI.create(AppConstants.API_V1 + "/financial-analysis/" + analysis.getId()))
            .body(ApiResponseDto.of(response, AppConstants.MESSAGE_SUCCESS));
    }

    @Operation(summary = "Buscar análise por id")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDto<FinancialAnalysisResponseDto>> findById(@PathVariable String id) {
        FinancialAnalysis analysis = getFinancialAnalysisByIdUseCase.handle(id);
        return ResponseEntity.ok(ApiResponseDto.of(analysisDtoMapper.toResponseDto(analysis),
            AppConstants.MESSAGE_SUCCESS));
    }

    @Operation(summary = "Listar histórico de análises com paginação")
    @GetMapping
    public ResponseEntity<ApiResponseDto<PageResponseDto<FinancialAnalysisResponseDto>>> list(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {
        PageResult<FinancialAnalysis> result = listFinancialAnalysesUseCase.handle(page, size);
        List<FinancialAnalysisResponseDto> content = result.content().stream()
            .map(analysisDtoMapper::toResponseDto).toList();
        PageResponseDto<FinancialAnalysisResponseDto> response = new PageResponseDto<>(content, result.page(),
            result.size(), result.totalElements(), result.totalPages());
        return ResponseEntity.ok(ApiResponseDto.of(response, AppConstants.MESSAGE_SUCCESS));
    }
}
