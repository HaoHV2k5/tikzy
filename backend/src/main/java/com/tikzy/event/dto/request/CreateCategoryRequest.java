package com.tikzy.event.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateCategoryRequest {

    @NotBlank(message = "Tên danh mục là bắt buộc")
    @Size(max = 100, message = "Tên danh mục không được vượt quá 100 ký tự")
    private String name;

    @NotBlank(message = "Slug danh mục là bắt buộc")
    @Size(max = 100, message = "Slug danh mục không được vượt quá 100 ký tự")
    @Pattern(
            regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$",
            message = "Slug chỉ gồm chữ thường không dấu, số và dấu gạch ngang")
    private String slug;

    @Size(max = 500, message = "URL icon không được vượt quá 500 ký tự")
    private String iconUrl;

    @Min(value = 0, message = "Thứ tự hiển thị không được nhỏ hơn 0")
    private Integer sortOrder;
}
