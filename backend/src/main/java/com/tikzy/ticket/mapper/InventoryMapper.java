package com.tikzy.ticket.mapper;

import com.tikzy.ticket.dto.response.InventoryResponse;
import com.tikzy.ticket.entity.ShowTimeTicketInventory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface InventoryMapper {

    @Mapping(source = "showTime.event.id", target = "eventId")
    @Mapping(source = "showTime.id", target = "showTimeId")
    @Mapping(source = "showTime.startTime", target = "showTimeStartTime")
    @Mapping(source = "showTime.endTime", target = "showTimeEndTime")
    @Mapping(source = "ticketType.id", target = "ticketTypeId")
    @Mapping(source = "ticketType.name", target = "ticketTypeName")
    @Mapping(target = "availableQuantity", expression = "java(available(inventory))")
    InventoryResponse toResponse(ShowTimeTicketInventory inventory);

    default int available(ShowTimeTicketInventory inventory) {
        int reserved = inventory.getReservedQuantity() == null ? 0 : inventory.getReservedQuantity();
        int sold = inventory.getSoldQuantity() == null ? 0 : inventory.getSoldQuantity();
        return inventory.getTotalQuantity() - reserved - sold;
    }
}
