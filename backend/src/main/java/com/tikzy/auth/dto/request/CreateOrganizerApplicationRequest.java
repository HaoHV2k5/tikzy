package com.tikzy.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateOrganizerApplicationRequest {

    @NotBlank(message = "Loại ban tổ chức là bắt buộc")
    @Pattern(regexp = "INDIVIDUAL|BUSINESS", message = "Loại ban tổ chức phải là INDIVIDUAL hoặc BUSINESS")
    private String organizerType;

    @NotBlank(message = "Tên ban tổ chức là bắt buộc")
    @Size(max = 255, message = "Tên ban tổ chức không được vượt quá 255 ký tự")
    private String organizerName;

    @Size(max = 50, message = "Số giấy tờ không được vượt quá 50 ký tự")
    private String identityNumber;

    @Size(max = 50, message = "Mã số thuế không được vượt quá 50 ký tự")
    private String taxCode;

    @NotBlank(message = "Số điện thoại liên hệ là bắt buộc")
    @Pattern(regexp = "^\\+?[0-9]{8,15}$", message = "Số điện thoại liên hệ không hợp lệ")
    private String contactPhone;

    @NotBlank(message = "Địa chỉ là bắt buộc")
    @Size(max = 500, message = "Địa chỉ không được vượt quá 500 ký tự")
    private String address;

    @Size(max = 500, message = "Website không được vượt quá 500 ký tự")
    private String websiteUrl;

    @Size(max = 2000, message = "Mô tả không được vượt quá 2000 ký tự")
    private String description;
}
