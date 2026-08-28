package com.tikzy.settlement.entity;

import com.tikzy.auth.entity.User;
import com.tikzy.common.entity.BaseEntity;
import com.tikzy.event.entity.Event;
import com.tikzy.settlement.enums.SettlementStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Bảng quyết toán Escrow: sau khi sự kiện kết thúc thành công,
 * Tikzy chốt sổ và chuyển tiền cho BTC (gross - refund - commission - ad_fee).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "settlements")
public class Settlement extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false, unique = true)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizer_id", nullable = false)
    private User organizer;

    @Column(name = "total_gross_revenue", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalGrossRevenue; // tổng tiền bán vé thu hộ thực tế

    @Builder.Default
    @Column(name = "total_refunded_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalRefundedAmount = BigDecimal.ZERO;

    @Column(name = "platform_commission_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal platformCommissionRate; // % phí Tikzy thu (vd: 8.00)

    @Column(name = "platform_commission_fee", nullable = false, precision = 18, scale = 2)
    private BigDecimal platformCommissionFee; // gross * rate%

    @Builder.Default
    @Column(name = "total_ad_fee_deducted", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalAdFeeDeducted = BigDecimal.ZERO; // các gói quảng cáo POSTPAID_ESCROW

    @Column(name = "net_payout_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal netPayoutAmount; // gross - refund - commission - ad_fee

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SettlementStatus status = SettlementStatus.PENDING;

    // TK nhận tiền của BTC
    @Column(name = "bank_name", length = 100)
    private String bankName;

    @Column(name = "bank_account_number", length = 50)
    private String bankAccountNumber;

    @Column(name = "bank_account_holder")
    private String bankAccountHolder;

    @Column(name = "bank_transfer_reference", length = 100)
    private String bankTransferReference; // mã UNC chuyển khoản

    @Column(name = "settled_at")
    private LocalDateTime settledAt; // ngày chuyển tiền
}
