package com.tikzy.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Base entity cho các bảng cần cả created_at lẫn updated_at (vd: refresh_tokens).
 */
@Getter
@Setter
@MappedSuperclass
public abstract class BaseAuditEntity extends BaseEntity {

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
