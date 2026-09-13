package com.tikzy.event.mapper;

import com.tikzy.event.dto.request.CreateShowTimeRequest;
import com.tikzy.event.dto.request.UpdateShowTimeRequest;
import com.tikzy.event.dto.response.ShowTimeResponse;
import com.tikzy.event.entity.ShowTime;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface ShowTimeMapper {

    @Mapping(target = "event", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    ShowTime toEntity(CreateShowTimeRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "event", ignore = true)
    void updateEntity(UpdateShowTimeRequest request, @MappingTarget ShowTime showTime);

    @Mapping(source = "event.id", target = "eventId")
    ShowTimeResponse toResponse(ShowTime showTime);
}
