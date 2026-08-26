package com.tikzy.ticket.repository;

import com.tikzy.ticket.entity.Ticket;
import com.tikzy.ticket.enums.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, UUID> {

    List<Ticket> findAllByOrderId(UUID orderId);

    Optional<Ticket> findByQrPayload(String qrPayload);

    long countByShowTimeIdAndStatus(UUID showTimeId, TicketStatus status);
}
