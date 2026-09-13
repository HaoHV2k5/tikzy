package com.tikzy.event.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateCategoryProposalRequest {

    @NotBlank(message = "Tên danh mục đề xuất là bắt buộc")
    @Size(max = 100, message = "Tên danh mục đề xuất không được vượt quá 100 ký tự")
    private String proposedName;

    @Size(max = 1000, message = "Mô tả không được vượt quá 1000 ký tự")
    private String description;
}
