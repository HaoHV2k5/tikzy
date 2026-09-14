package com.tikzy.event.mapper;

import com.tikzy.event.dto.request.CreateTicketTypeRequest;
import com.tikzy.event.dto.request.UpdateTicketTypeRequest;
import com.tikzy.event.dto.response.TicketTypeResponse;
import com.tikzy.event.entity.TicketType;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface TicketTypeMapper {

    @Mapping(target = "event", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    TicketType toEntity(CreateTicketTypeRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "event", ignore = true)
    void updateEntity(UpdateTicketTypeRequest request, @MappingTarget TicketType ticketType);

    @Mapping(source = "event.id", target = "eventId")
    TicketTypeResponse toResponse(TicketType ticketType);
}
