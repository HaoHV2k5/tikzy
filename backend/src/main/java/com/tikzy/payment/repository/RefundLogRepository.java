package com.tikzy.payment.repository;

import com.tikzy.payment.entity.RefundLog;
import com.tikzy.payment.enums.RefundStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RefundLogRepository extends JpaRepository<RefundLog, UUID> {

    List<RefundLog> findAllByOrderId(UUID orderId);

    List<RefundLog> findAllByEventId(UUID eventId);

    List<RefundLog> findAllByStatus(RefundStatus status);
}
