package com.finance.adapters.in.rest.v1;

import com.finance.application.dto.CreateIncomeDto;
import com.finance.application.dto.IncomeResponseDto;
import com.finance.application.dto.UpdateIncomeDto;
import com.finance.application.mapper.IncomeDtoMapper;
import com.finance.application.query.IncomeSearchQuery;
import com.finance.application.query.PageResult;
import com.finance.domain.model.Income;
import com.finance.domain.ports.in.CreateIncomeUseCase;
import com.finance.domain.ports.in.DeleteIncomeUseCase;
import com.finance.domain.ports.in.GetIncomeByIdUseCase;
import com.finance.domain.ports.in.ListIncomesUseCase;
import com.finance.domain.ports.in.UpdateIncomeUseCase;
import com.finance.shared.constants.AppConstants;
import com.finance.shared.response.ApiResponseDto;
import com.finance.shared.response.PageResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
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
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping(AppConstants.API_V1 + "/incomes")
@Tag(name = "Receitas", description = "Gerenciamento de receitas")
public class IncomeController {

    private final CreateIncomeUseCase createIncomeUseCase;
    private final UpdateIncomeUseCase updateIncomeUseCase;
    private final DeleteIncomeUseCase deleteIncomeUseCase;
    private final GetIncomeByIdUseCase getIncomeByIdUseCase;
    private final ListIncomesUseCase listIncomesUseCase;
    private final IncomeDtoMapper incomeDtoMapper;

    public IncomeController(CreateIncomeUseCase createIncomeUseCase, UpdateIncomeUseCase updateIncomeUseCase,
                            DeleteIncomeUseCase deleteIncomeUseCase, GetIncomeByIdUseCase getIncomeByIdUseCase,
                            ListIncomesUseCase listIncomesUseCase, IncomeDtoMapper incomeDtoMapper) {
        this.createIncomeUseCase = createIncomeUseCase;
        this.updateIncomeUseCase = updateIncomeUseCase;
        this.deleteIncomeUseCase = deleteIncomeUseCase;
        this.getIncomeByIdUseCase = getIncomeByIdUseCase;
        this.listIncomesUseCase = listIncomesUseCase;
        this.incomeDtoMapper = incomeDtoMapper;
    }

    @Operation(summary = "Registrar uma nova receita")
    @PostMapping
    public ResponseEntity<ApiResponseDto<IncomeResponseDto>> create(@Valid @RequestBody CreateIncomeDto dto) {
        Income income = createIncomeUseCase.handle(incomeDtoMapper.toCommand(dto));
        IncomeResponseDto response = incomeDtoMapper.toResponseDto(income);
        return ResponseEntity.created(URI.create(AppConstants.API_V1 + "/incomes/" + income.getId()))
            .body(ApiResponseDto.of(response, AppConstants.MESSAGE_SUCCESS));
    }

    @Operation(summary = "Atualizar uma receita existente")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponseDto<IncomeResponseDto>> update(@PathVariable String id,
                                                                    @Valid @RequestBody UpdateIncomeDto dto) {
        Income income = updateIncomeUseCase.handle(id, incomeDtoMapper.toCommand(dto));
        return ResponseEntity.ok(ApiResponseDto.of(incomeDtoMapper.toResponseDto(income),
            AppConstants.MESSAGE_SUCCESS));
    }

    @Operation(summary = "Excluir uma receita (exclusão lógica)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        deleteIncomeUseCase.handle(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Buscar receita por id")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDto<IncomeResponseDto>> findById(@PathVariable String id) {
        Income income = getIncomeByIdUseCase.handle(id);
        return ResponseEntity.ok(ApiResponseDto.of(incomeDtoMapper.toResponseDto(income),
            AppConstants.MESSAGE_SUCCESS));
    }

    @Operation(summary = "Listar receitas com filtros, paginação e ordenação")
    @GetMapping
    public ResponseEntity<ApiResponseDto<PageResponseDto<IncomeResponseDto>>> list(
        @RequestParam(required = false) String accountId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
        @PageableDefault(size = 20, sort = "receivedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResult<Income> result = listIncomesUseCase.handle(new IncomeSearchQuery(null, accountId, startDate, endDate,
            pageable));
        List<IncomeResponseDto> content = result.content().stream().map(incomeDtoMapper::toResponseDto).toList();
        PageResponseDto<IncomeResponseDto> page = new PageResponseDto<>(content, result.page(), result.size(),
            result.totalElements(), result.totalPages());
        return ResponseEntity.ok(ApiResponseDto.of(page, AppConstants.MESSAGE_SUCCESS));
    }
}
