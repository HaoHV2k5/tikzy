package com.tikzy.event.entity;

import com.tikzy.auth.entity.User;
import com.tikzy.common.entity.BaseEntity;
import com.tikzy.event.enums.EventStatus;
import com.tikzy.event.enums.RefundPolicy;
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

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "events")
public class Event extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizer_id", nullable = false)
    private User organizer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "venue_name")
    private String venueName;

    @Column(name = "venue_address", length = 500)
    private String venueAddress;

    @Column(name = "banner_url", length = 500)
    private String bannerUrl; // Cloudinary

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl; // Cloudinary

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EventStatus status = EventStatus.DRAFT;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "refund_policy", nullable = false, length = 20)
    private RefundPolicy refundPolicy = RefundPolicy.NO_REFUND;

    @Column(name = "refund_deadline_days")
    private Integer refundDeadlineDays; // số ngày trước show còn được hoàn

    @Column(name = "refund_fee_percentage", precision = 5, scale = 2)
    private BigDecimal refundFeePercentage; // phí hoàn vé (%)

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;
}
