package com.tikzy.ticket.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UpsertInventoriesRequest {

    @NotEmpty(message = "Danh sách tồn kho là bắt buộc")
    @Valid
    private List<UpsertInventoryItemRequest> items;
}
