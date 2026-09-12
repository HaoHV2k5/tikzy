package com.tikzy.event.repository;

import com.tikzy.event.entity.CategoryRequest;
import com.tikzy.event.enums.CategoryRequestStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryRequestRepository extends JpaRepository<CategoryRequest, UUID> {

    Page<CategoryRequest> findAllByStatus(
            CategoryRequestStatus status,
            Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT cr FROM CategoryRequest cr WHERE cr.id = :id")
    Optional<CategoryRequest> findByIdForUpdate(@Param("id") UUID id);
}
