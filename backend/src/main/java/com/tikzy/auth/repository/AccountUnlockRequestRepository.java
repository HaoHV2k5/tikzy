package com.tikzy.auth.repository;

import com.tikzy.auth.enums.AccountRecoveryType;
import com.tikzy.auth.entity.AccountUnlockRequest;
import com.tikzy.auth.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountUnlockRequestRepository extends JpaRepository<AccountUnlockRequest, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<AccountUnlockRequest> findFirstByUserAndRequestTypeAndConsumedAtIsNullAndOtpVerifiedAtIsNullOrderByCreatedAtDesc(
            User user,
            AccountRecoveryType requestType);

    Optional<AccountUnlockRequest> findByResetTokenHashAndRequestType(
            String resetTokenHash,
            AccountRecoveryType requestType);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM AccountUnlockRequest r "
            + "WHERE r.resetTokenHash = :resetTokenHash AND r.requestType = :requestType")
    Optional<AccountUnlockRequest> findByResetTokenHashAndRequestTypeForUpdate(
            @Param("resetTokenHash") String resetTokenHash,
            @Param("requestType") AccountRecoveryType requestType);

    @Modifying
    @Query("UPDATE AccountUnlockRequest r SET r.consumedAt = :consumedAt "
            + "WHERE r.user = :user AND r.requestType = :requestType AND r.consumedAt IS NULL")
    int consumeActiveByUserAndRequestType(
            @Param("user") User user,
            @Param("requestType") AccountRecoveryType requestType,
            @Param("consumedAt") LocalDateTime consumedAt);
}
