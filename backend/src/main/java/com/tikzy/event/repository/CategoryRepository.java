package com.tikzy.event.repository;

import com.tikzy.event.entity.Category;
import com.tikzy.event.enums.CategoryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {

    Optional<Category> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, UUID id);

    boolean existsByNormalizedNameAndStatusIn(
            String normalizedName,
            Collection<CategoryStatus> statuses);

    boolean existsByNormalizedNameAndStatusInAndIdNot(
            String normalizedName,
            Collection<CategoryStatus> statuses,
            UUID id);

    Optional<Category> findFirstByNormalizedNameAndStatusIn(
            String normalizedName,
            Collection<CategoryStatus> statuses);

    Page<Category> findAllByStatus(CategoryStatus status, Pageable pageable);

    @Query(
            value = "SELECT pg_advisory_xact_lock(hashtext(:normalizedName))",
            nativeQuery = true)
    void lockNormalizedName(@Param("normalizedName") String normalizedName);
}
