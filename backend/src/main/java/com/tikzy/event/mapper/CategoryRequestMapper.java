package com.tikzy.event.mapper;

import com.tikzy.event.dto.request.CreateCategoryProposalRequest;
import com.tikzy.event.dto.response.CategoryRequestResponse;
import com.tikzy.event.entity.CategoryRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = CategoryMapper.class)
public interface CategoryRequestMapper {

    @Mapping(target = "requester", ignore = true)
    @Mapping(target = "normalizedProposedName", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "reviewNote", ignore = true)
    @Mapping(target = "reviewedBy", ignore = true)
    @Mapping(target = "reviewedAt", ignore = true)
    @Mapping(target = "category", ignore = true)
    CategoryRequest toEntity(CreateCategoryProposalRequest request);

    @Mapping(source = "requester.id", target = "requesterId")
    @Mapping(source = "requester.email", target = "requesterEmail")
    @Mapping(source = "reviewedBy.id", target = "reviewedBy")
    CategoryRequestResponse toResponse(CategoryRequest request);
}
