package com.tikzy.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewOrganizerApplicationRequest {

    @NotBlank(message = "Kết quả duyệt là bắt buộc")
    @Pattern(regexp = "APPROVED|REJECTED", message = "Kết quả duyệt phải là APPROVED hoặc REJECTED")
    private String status;

    @Size(max = 1000, message = "Ghi chú duyệt không được vượt quá 1000 ký tự")
    private String reviewNote;
}
