package com.finance.adapters.in.rest.v1;

import com.finance.application.dto.CategoryResponseDto;
import com.finance.application.dto.CreateCategoryDto;
import com.finance.application.dto.UpdateCategoryDto;
import com.finance.application.mapper.CategoryDtoMapper;
import com.finance.application.query.CategorySearchQuery;
import com.finance.application.query.PageResult;
import com.finance.domain.enums.CategoryClassification;
import com.finance.domain.model.Category;
import com.finance.domain.ports.in.CreateCategoryUseCase;
import com.finance.domain.ports.in.DeleteCategoryUseCase;
import com.finance.domain.ports.in.GetCategoryByIdUseCase;
import com.finance.domain.ports.in.ListCategoriesUseCase;
import com.finance.domain.ports.in.UpdateCategoryUseCase;
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
@RequestMapping(AppConstants.API_V1 + "/categories")
@Tag(name = "Categorias", description = "Gerenciamento de categorias financeiras")
public class CategoryController {

    private final CreateCategoryUseCase createCategoryUseCase;
    private final UpdateCategoryUseCase updateCategoryUseCase;
    private final DeleteCategoryUseCase deleteCategoryUseCase;
    private final GetCategoryByIdUseCase getCategoryByIdUseCase;
    private final ListCategoriesUseCase listCategoriesUseCase;
    private final CategoryDtoMapper categoryDtoMapper;

    public CategoryController(CreateCategoryUseCase createCategoryUseCase, UpdateCategoryUseCase updateCategoryUseCase,
                              DeleteCategoryUseCase deleteCategoryUseCase, GetCategoryByIdUseCase getCategoryByIdUseCase,
                              ListCategoriesUseCase listCategoriesUseCase, CategoryDtoMapper categoryDtoMapper) {
        this.createCategoryUseCase = createCategoryUseCase;
        this.updateCategoryUseCase = updateCategoryUseCase;
        this.deleteCategoryUseCase = deleteCategoryUseCase;
        this.getCategoryByIdUseCase = getCategoryByIdUseCase;
        this.listCategoriesUseCase = listCategoriesUseCase;
        this.categoryDtoMapper = categoryDtoMapper;
    }

    @Operation(summary = "Criar uma nova categoria")
    @PostMapping
    public ResponseEntity<ApiResponseDto<CategoryResponseDto>> create(
        @Valid @RequestBody CreateCategoryDto request) {
        Category category = createCategoryUseCase.handle(categoryDtoMapper.toCommand(request));
        CategoryResponseDto response = categoryDtoMapper.toResponseDto(category);
        return ResponseEntity.created(URI.create(AppConstants.API_V1 + "/categories/" + category.getId()))
            .body(ApiResponseDto.of(response, AppConstants.MESSAGE_SUCCESS));
    }

    @Operation(summary = "Buscar categoria por id")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDto<CategoryResponseDto>> findById(@PathVariable String id) {
        Category category = getCategoryByIdUseCase.handle(id);
        return ResponseEntity.ok(ApiResponseDto.of(categoryDtoMapper.toResponseDto(category),
            AppConstants.MESSAGE_SUCCESS));
    }

    @Operation(summary = "Listar categorias com filtros, paginação e ordenação")
    @GetMapping
    public ResponseEntity<ApiResponseDto<PageResponseDto<CategoryResponseDto>>> list(
        @RequestParam(required = false) CategoryClassification classification,
        @RequestParam(required = false) Boolean active,
        @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        PageResult<Category> result = listCategoriesUseCase.handle(new CategorySearchQuery(classification, active,
            pageable));
        List<CategoryResponseDto> content = result.content().stream().map(categoryDtoMapper::toResponseDto).toList();
        PageResponseDto<CategoryResponseDto> page = new PageResponseDto<>(content, result.page(), result.size(),
            result.totalElements(), result.totalPages());
        return ResponseEntity.ok(ApiResponseDto.of(page, AppConstants.MESSAGE_SUCCESS));
    }

    @Operation(summary = "Atualizar uma categoria existente")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponseDto<CategoryResponseDto>> update(@PathVariable String id,
        @Valid @RequestBody UpdateCategoryDto request) {
        Category category = updateCategoryUseCase.handle(id, categoryDtoMapper.toCommand(request));
        return ResponseEntity.ok(ApiResponseDto.of(categoryDtoMapper.toResponseDto(category),
            AppConstants.MESSAGE_SUCCESS));
    }

    @Operation(summary = "Excluir uma categoria (exclusão lógica)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        deleteCategoryUseCase.handle(id);
        return ResponseEntity.noContent().build();
    }
}
