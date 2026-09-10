package com.tikzy.auth.repository;

import com.tikzy.auth.entity.OrganizerApplication;
import com.tikzy.auth.enums.OrganizerApplicationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrganizerApplicationRepository extends JpaRepository<OrganizerApplication, UUID> {

    boolean existsByApplicantIdAndStatus(UUID applicantId, OrganizerApplicationStatus status);

    boolean existsByIdentityNumberAndStatusIn(
            String identityNumber,
            Collection<OrganizerApplicationStatus> statuses);

    boolean existsByTaxCodeAndStatusIn(
            String taxCode,
            Collection<OrganizerApplicationStatus> statuses);

    Optional<OrganizerApplication> findFirstByApplicantIdOrderByCreatedAtDesc(UUID applicantId);

    Page<OrganizerApplication> findAllByStatus(
            OrganizerApplicationStatus status,
            Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT oa FROM OrganizerApplication oa WHERE oa.id = :id")
    Optional<OrganizerApplication> findByIdForUpdate(@Param("id") UUID id);
}
