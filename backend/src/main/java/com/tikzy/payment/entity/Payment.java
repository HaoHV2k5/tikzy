package com.tikzy.payment.entity;

import com.tikzy.common.entity.BaseEntity;
import com.tikzy.order.entity.Order;
import com.tikzy.payment.enums.PaymentMethod;
import com.tikzy.payment.enums.PaymentStatus;
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

/**
 * 1 đơn hàng có thể thử thanh toán nhiều lần (orders 1 - N payments):
 * trong 15 phút giữ chỗ, khách có thể đổi cổng thanh toán nếu gặp sự cố.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "payments")
public class Payment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false, length = 20)
    private PaymentMethod method;

    @Column(name = "transaction_id")
    private String transactionId; // mã GD từ cổng TT (lượt thành công mới có mã chốt)

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount; // bằng đúng orders.total_amount

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;
}
