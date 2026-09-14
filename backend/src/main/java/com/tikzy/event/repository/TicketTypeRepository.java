package com.tikzy.event.repository;

import com.tikzy.event.entity.TicketType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TicketTypeRepository extends JpaRepository<TicketType, UUID> {

    List<TicketType> findAllByEventIdAndIsActiveTrue(UUID eventId);

    Optional<TicketType> findByIdAndEventId(UUID id, UUID eventId);

    Page<TicketType> findAllByEventId(UUID eventId, Pageable pageable);

    Page<TicketType> findAllByEventIdAndIsActive(UUID eventId, Boolean isActive, Pageable pageable);

    boolean existsByEventIdAndNameIgnoreCase(UUID eventId, String name);

    boolean existsByEventIdAndNameIgnoreCaseAndIdNot(UUID eventId, String name, UUID id);
}
