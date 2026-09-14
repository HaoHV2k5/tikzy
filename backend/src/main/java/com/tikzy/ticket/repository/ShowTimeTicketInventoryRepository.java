package com.tikzy.ticket.repository;

import com.tikzy.ticket.entity.ShowTimeTicketInventory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShowTimeTicketInventoryRepository extends JpaRepository<ShowTimeTicketInventory, UUID> {

    List<ShowTimeTicketInventory> findAllByShowTimeId(UUID showTimeId);

    List<ShowTimeTicketInventory> findAllByShowTimeEventId(UUID eventId);

    Optional<ShowTimeTicketInventory> findByShowTimeIdAndTicketTypeId(UUID showTimeId, UUID ticketTypeId);

    Optional<ShowTimeTicketInventory> findByIdAndShowTimeEventId(UUID id, UUID eventId);

    Page<ShowTimeTicketInventory> findAllByShowTimeEventId(UUID eventId, Pageable pageable);

    Page<ShowTimeTicketInventory> findAllByShowTimeEventIdAndShowTimeId(
            UUID eventId,
            UUID showTimeId,
            Pageable pageable);

    Page<ShowTimeTicketInventory> findAllByShowTimeEventIdAndTicketTypeId(
            UUID eventId,
            UUID ticketTypeId,
            Pageable pageable);

    Page<ShowTimeTicketInventory> findAllByShowTimeEventIdAndShowTimeIdAndTicketTypeId(
            UUID eventId,
            UUID showTimeId,
            UUID ticketTypeId,
            Pageable pageable);

    boolean existsByShowTimeIdAndTicketTypeId(UUID showTimeId, UUID ticketTypeId);
}
