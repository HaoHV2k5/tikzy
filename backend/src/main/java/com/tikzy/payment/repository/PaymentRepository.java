package com.tikzy.payment.repository;

import com.tikzy.payment.entity.Payment;
import com.tikzy.payment.enums.PaymentMethod;
import com.tikzy.payment.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    List<Payment> findAllByOrderId(UUID orderId);

    Optional<Payment> findByMethodAndTransactionId(PaymentMethod method, String transactionId);

    /**
     * Payment Reconciliation Scheduler: tìm các payment PENDING quá 15 phút
     * để chủ động gọi Query API sang cổng thanh toán đồng bộ trạng thái.
     */
    List<Payment> findAllByStatusAndCreatedAtBefore(PaymentStatus status, LocalDateTime before);
}
