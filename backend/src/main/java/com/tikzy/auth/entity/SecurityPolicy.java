package com.tikzy.auth.entity;

import com.tikzy.common.entity.BaseAuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "security_policies")
public class SecurityPolicy extends BaseAuditEntity {

    @Builder.Default
    @Column(name = "max_failed_login_attempts", nullable = false)
    private Integer maxFailedLoginAttempts = 5;
}
