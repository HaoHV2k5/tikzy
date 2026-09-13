package com.tikzy.event.repository;

import com.tikzy.event.entity.Event;
import com.tikzy.event.enums.EventStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EventRepository extends JpaRepository<Event, UUID> {

    Page<Event> findAllByStatus(EventStatus status, Pageable pageable);

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

    Page<Event> findAllByCategoryIdAndStatus(UUID categoryId, EventStatus status, Pageable pageable);
}
