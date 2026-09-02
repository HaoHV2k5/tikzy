package com.tikzy.auth.repository;

import com.tikzy.auth.entity.SecurityPolicy;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SecurityPolicyRepository extends JpaRepository<SecurityPolicy, UUID> {

    Optional<SecurityPolicy> findFirstByOrderByCreatedAtAsc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM SecurityPolicy p ORDER BY p.createdAt ASC")
    Optional<SecurityPolicy> findFirstForUpdate();
}
