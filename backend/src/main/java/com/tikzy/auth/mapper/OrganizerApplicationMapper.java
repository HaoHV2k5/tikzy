package com.tikzy.auth.mapper;

import com.tikzy.auth.dto.response.OrganizerApplicationResponse;
import com.tikzy.auth.entity.OrganizerApplication;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrganizerApplicationMapper {

    @Mapping(source = "applicant.id", target = "applicantId")
    @Mapping(source = "applicant.email", target = "applicantEmail")
    @Mapping(source = "reviewedBy.id", target = "reviewedBy")
    OrganizerApplicationResponse toResponse(OrganizerApplication application);
}
