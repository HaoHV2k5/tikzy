package com.tikzy.ticket.repository;

import com.tikzy.ticket.entity.ShowTimeTicketInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShowTimeTicketInventoryRepository extends JpaRepository<ShowTimeTicketInventory, UUID> {

    List<ShowTimeTicketInventory> findAllByShowTimeId(UUID showTimeId);

    Optional<ShowTimeTicketInventory> findByShowTimeIdAndTicketTypeId(UUID showTimeId, UUID ticketTypeId);
}
