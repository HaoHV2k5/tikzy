package com.tikzy.event.mapper;

import com.tikzy.event.dto.request.CreateCategoryRequest;
import com.tikzy.event.dto.request.UpdateCategoryRequest;
import com.tikzy.event.dto.response.CategoryResponse;
import com.tikzy.event.entity.Category;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    @Mapping(target = "normalizedName", ignore = true)
    @Mapping(target = "status", ignore = true)
    Category toEntity(CreateCategoryRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "normalizedName", ignore = true)
    @Mapping(target = "status", ignore = true)
    void updateEntity(UpdateCategoryRequest request, @MappingTarget Category category);

    CategoryResponse toResponse(Category category);
}
