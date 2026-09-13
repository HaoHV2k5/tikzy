package com.tikzy.event.repository;

import com.tikzy.event.entity.ShowTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShowTimeRepository extends JpaRepository<ShowTime, UUID> {

    List<ShowTime> findAllByEventIdAndIsActiveTrueOrderByStartTimeAsc(UUID eventId);

    Optional<ShowTime> findByIdAndEventId(UUID id, UUID eventId);

    Page<ShowTime> findAllByEventId(UUID eventId, Pageable pageable);

    Page<ShowTime> findAllByEventIdAndIsActive(UUID eventId, Boolean isActive, Pageable pageable);

    boolean existsByEventIdAndStartTimeAndEndTime(
            UUID eventId,
            LocalDateTime startTime,
            LocalDateTime endTime);

    @Query("""
            SELECT COUNT(st) > 0
            FROM ShowTime st
            WHERE st.event.id = :eventId
              AND st.isActive = true
              AND st.startTime < :endTime
              AND st.endTime > :startTime
            """)
    boolean existsOverlapping(
            @Param("eventId") UUID eventId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    @Query("""
            SELECT COUNT(st) > 0
            FROM ShowTime st
            WHERE st.event.id = :eventId
              AND st.isActive = true
              AND st.id <> :excludeId
              AND st.startTime < :endTime
              AND st.endTime > :startTime
            """)
    boolean existsOverlappingExcludingId(
            @Param("eventId") UUID eventId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("excludeId") UUID excludeId);
}
