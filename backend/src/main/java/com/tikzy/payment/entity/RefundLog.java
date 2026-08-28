package com.tikzy.payment.entity;

import com.tikzy.common.entity.BaseEntity;
import com.tikzy.event.entity.Event;
import com.tikzy.order.entity.Order;
import com.tikzy.payment.enums.RefundStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "refund_logs")
public class RefundLog extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount; // = orders.total_amount (chỉ hoàn tiền thực trả)

    @Column(name = "payment_method", length = 20)
    private String paymentMethod;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RefundStatus status = RefundStatus.PROCESSING;

    @Column(name = "gateway_refund_id")
    private String gatewayRefundId; // mã hoàn tiền từ cổng

    @Builder.Default
    @Column(name = "idempotency_key", nullable = false, unique = true, updatable = false)
    private UUID idempotencyKey = UUID.randomUUID();

    @Column(name = "refund_reason", length = 500)
    private String refundReason;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    // Dự phòng chuyển tay khi API refund lỗi / quá hạn 90 ngày
    @Column(name = "bank_account_number", length = 50)
    private String bankAccountNumber;

    @Column(name = "bank_name", length = 100)
    private String bankName;

    @Column(name = "bank_account_holder")
    private String bankAccountHolder;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}
