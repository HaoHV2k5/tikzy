package com.tikzy.event.repository;

import com.tikzy.event.entity.Event;
import com.tikzy.event.enums.EventStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface EventRepository extends JpaRepository<Event, UUID> {

    Page<Event> findAllByStatus(EventStatus status, Pageable pageable);

    Page<Event> findAllByOrganizerId(UUID organizerId, Pageable pageable);

    Page<Event> findAllByCategoryIdAndStatus(UUID categoryId, EventStatus status, Pageable pageable);
}
