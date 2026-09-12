package com.tikzy.event.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewCategoryRequest {

    @NotBlank(message = "Kết quả xử lý là bắt buộc")
    @Pattern(
            regexp = "APPROVED|REJECTED",
            message = "Kết quả xử lý phải là APPROVED hoặc REJECTED")
    private String status;

    @Size(max = 1000, message = "Ghi chú xử lý không được vượt quá 1000 ký tự")
    private String reviewNote;

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
