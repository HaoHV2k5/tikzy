package com.tikzy.auth.dto.request;

import com.tikzy.common.exception.AppException;
import com.tikzy.common.exception.ErrorCode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Getter
@Setter
@Schema(description = "Phân trang danh sách hồ sơ đăng ký ban tổ chức")
public class OrganizerApplicationPageRequest {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;
    private static final String DEFAULT_SORT_PROPERTY = "createdAt";
    private static final Set<String> ALLOWED_SORT_PROPERTIES = Set.of(
            "createdAt",
            "status",
            "organizerName",
            "organizerType",
            "reviewedAt");

    @Schema(description = "Lọc theo trạng thái hồ sơ", example = "PENDING")
    private String status;

    @Schema(description = "Số trang, bắt đầu từ 0", example = "0")
    private Integer page;

    @Schema(description = "Số phần tử mỗi trang", example = "10")
    private Integer size;

    @Schema(description = "Sắp xếp theo dạng property,direction", example = "createdAt,desc")
    private List<String> sort;

    public Pageable toPageable() {
        int pageNumber = page == null ? DEFAULT_PAGE : page;
        int pageSize = size == null ? DEFAULT_SIZE : size;
        if (pageNumber < 0) {
            throw new AppException(ErrorCode.INVALID_ORGANIZER_APPLICATION, "Số trang phải lớn hơn hoặc bằng 0");
        }
        if (pageSize < 1 || pageSize > MAX_SIZE) {
            throw new AppException(
                    ErrorCode.INVALID_ORGANIZER_APPLICATION,
                    "Kích thước trang phải từ 1 đến 100");
        }
        return PageRequest.of(pageNumber, pageSize, parseSort(sort));
    }

    static Sort parseSort(List<String> sortValues) {
        if (sortValues == null || sortValues.isEmpty()) {
            return defaultSort();
        }

        List<Sort.Order> orders = new ArrayList<>();
        String pendingProperty = null;
        for (String raw : sortValues) {
            if (!StringUtils.hasText(raw)) {
                continue;
            }
            String value = raw.trim();
            if (value.contains(",")) {
                flushPending(orders, pendingProperty);
                pendingProperty = null;
                String[] parts = value.split(",", 2);
                orders.add(order(parts[0], parts[1]));
                continue;
            }

            Optional<Sort.Direction> direction = Sort.Direction.fromOptionalString(value);
            if (direction.isPresent()) {
                if (pendingProperty == null) {
                    throw invalidSort(value);
                }
                orders.add(new Sort.Order(direction.get(), pendingProperty));
                pendingProperty = null;
                continue;
            }

            flushPending(orders, pendingProperty);
            pendingProperty = normalizeProperty(value);
        }
        flushPending(orders, pendingProperty);
        return orders.isEmpty() ? defaultSort() : Sort.by(orders);
    }

    private static void flushPending(List<Sort.Order> orders, String pendingProperty) {
        if (pendingProperty != null) {
            orders.add(new Sort.Order(Sort.Direction.DESC, pendingProperty));
        }
    }

    private static Sort.Order order(String property, String directionValue) {
        Sort.Direction direction = Sort.Direction.fromOptionalString(directionValue.trim())
                .orElseThrow(() -> invalidSort(directionValue));
        return new Sort.Order(direction, normalizeProperty(property));
    }

    private static String normalizeProperty(String property) {
        String normalized = property.trim();
        if (!ALLOWED_SORT_PROPERTIES.contains(normalized)) {
            throw invalidSort(normalized);
        }
        return normalized;
    }

    private static Sort defaultSort() {
        return Sort.by(Sort.Direction.DESC, DEFAULT_SORT_PROPERTY);
    }

    private static AppException invalidSort(String value) {
        return new AppException(
                ErrorCode.INVALID_ORGANIZER_APPLICATION,
                "Trường sắp xếp không hợp lệ: " + value.trim() + ". Dùng createdAt,desc");
    }
}
