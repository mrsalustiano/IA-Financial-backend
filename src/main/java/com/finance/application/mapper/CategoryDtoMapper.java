package com.finance.application.mapper;

import com.finance.application.command.CreateCategoryCommand;
import com.finance.application.command.UpdateCategoryCommand;
import com.finance.application.dto.CategoryResponseDto;
import com.finance.application.dto.CreateCategoryDto;
import com.finance.application.dto.UpdateCategoryDto;
import com.finance.domain.model.Category;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CategoryDtoMapper {

    CreateCategoryCommand toCommand(CreateCategoryDto dto);

    UpdateCategoryCommand toCommand(UpdateCategoryDto dto);

    CategoryResponseDto toResponseDto(Category category);
}
