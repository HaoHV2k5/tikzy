package com.tikzy.auth.entity;

import com.tikzy.auth.enums.AccountRecoveryType;
import com.tikzy.common.entity.BaseAuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "account_unlock_requests")
public class AccountUnlockRequest extends BaseAuditEntity {

    @OnDelete(action = OnDeleteAction.CASCADE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "request_type", nullable = false, length = 30)
    private AccountRecoveryType requestType = AccountRecoveryType.ACCOUNT_UNLOCK;

    @Column(name = "otp_hash", nullable = false, length = 255)
    private String otpHash;

    @Builder.Default
    @Column(name = "otp_attempts", nullable = false)
    private Integer otpAttempts = 0;

    @Column(name = "otp_expires_at", nullable = false)
    private LocalDateTime otpExpiresAt;

    @Column(name = "reset_token_hash", unique = true, length = 64)
    private String resetTokenHash;

    @Column(name = "reset_token_expires_at")
    private LocalDateTime resetTokenExpiresAt;

    @Column(name = "otp_verified_at")
    private LocalDateTime otpVerifiedAt;

    @Column(name = "consumed_at")
    private LocalDateTime consumedAt;
}
