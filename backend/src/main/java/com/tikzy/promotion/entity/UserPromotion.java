package com.tikzy.promotion.entity;

import com.tikzy.auth.entity.User;
import com.tikzy.broadcast.entity.EventBroadcast;
import com.tikzy.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "user_promotions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "promotion_id"})
)
public class UserPromotion extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promotion_id", nullable = false)
    private Promotion promotion;

    @Column(name = "source", nullable = false, length = 30)
    private String source;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_broadcast_id")
    private EventBroadcast sourceBroadcast;

    @Builder.Default
    @Column(name = "status", nullable = false, length = 20)
    private String status = "AVAILABLE";

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;
}
