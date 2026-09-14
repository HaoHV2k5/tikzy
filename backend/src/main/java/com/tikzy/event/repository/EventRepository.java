package com.tikzy.event.repository;

import com.tikzy.event.entity.Event;
import com.tikzy.event.enums.EventStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EventRepository extends JpaRepository<Event, UUID> {

    @EntityGraph(attributePaths = {"category", "organizer"})
    Page<Event> findAllByStatus(EventStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"category", "organizer"})
    Optional<Event> findByIdAndStatus(UUID id, EventStatus status);

    @EntityGraph(attributePaths = {"category", "organizer"})
    Page<Event> findAllByOrganizerId(UUID organizerId, Pageable pageable);

    @EntityGraph(attributePaths = {"category", "organizer"})
    Page<Event> findAllByOrganizerIdAndStatus(
            UUID organizerId,
            EventStatus status,
            Pageable pageable);

    @EntityGraph(attributePaths = {"category", "organizer"})
    Optional<Event> findByIdAndOrganizerId(UUID id, UUID organizerId);

    boolean existsByOrganizerIdAndTitle(UUID organizerId, String title);

    Optional<Event> findByOrganizerIdAndTitle(UUID organizerId, String title);

    @EntityGraph(attributePaths = {"category", "organizer"})
    Page<Event> findAllByCategoryIdAndStatus(UUID categoryId, EventStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"category", "organizer"})
    @Query("""
            SELECT e
            FROM Event e
            WHERE e.status = :status
              AND (
                    :keyword IS NULL
                    OR LOWER(e.title) LIKE :keyword
                    OR LOWER(COALESCE(e.description, '')) LIKE :keyword
                    OR LOWER(COALESCE(e.venueName, '')) LIKE :keyword
                    OR LOWER(COALESCE(e.venueAddress, '')) LIKE :keyword
                  )
              AND (
                    :location IS NULL
                    OR LOWER(COALESCE(e.venueName, '')) LIKE :location
                    OR LOWER(COALESCE(e.venueAddress, '')) LIKE :location
                  )
              AND (
                    :fromTime IS NULL AND :toTime IS NULL
                    OR EXISTS (
                        SELECT 1
                        FROM ShowTime st
                        WHERE st.event.id = e.id
                          AND st.isActive = TRUE
                          AND (:fromTime IS NULL OR st.startTime >= :fromTime)
                          AND (:toTime IS NULL OR st.startTime <= :toTime)
                    )
                  )
              AND (
                    :minPrice IS NULL AND :maxPrice IS NULL
                    OR EXISTS (
                        SELECT 1
                        FROM TicketType tt
                        WHERE tt.event.id = e.id
                          AND tt.isActive = TRUE
                          AND (:minPrice IS NULL OR tt.price >= :minPrice)
                          AND (:maxPrice IS NULL OR tt.price <= :maxPrice)
                    )
                  )
            """)
    Page<Event> searchPublished(
            @Param("status") EventStatus status,
            @Param("keyword") String keyword,
            @Param("location") String location,
            @Param("fromTime") LocalDateTime fromTime,
            @Param("toTime") LocalDateTime toTime,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            Pageable pageable);
}
