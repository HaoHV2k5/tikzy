package com.tikzy.broadcast.entity;

import com.tikzy.auth.entity.User;
import com.tikzy.broadcast.enums.TargetAudience;
import com.tikzy.common.entity.BaseEntity;
import com.tikzy.event.entity.Event;
import com.tikzy.promotion.entity.Promotion;
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

import java.time.LocalDateTime;

/**
 * Công cụ Organizer Broadcast: BTC gửi thư xin lỗi + phát voucher đền bù
 * cho khách (đặc biệt nhóm từng dùng voucher) khi show bị hủy.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "event_broadcasts")
public class EventBroadcast extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizer_id", nullable = false)
    private User organizer;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_audience", nullable = false, length = 30)
    private TargetAudience targetAudience;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "message_content", nullable = false, columnDefinition = "TEXT")
    private String messageContent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attached_compensation_promotion_id")
    private Promotion attachedCompensationPromotion;

    @Builder.Default
    @Column(name = "total_recipients", nullable = false)
    private Integer totalRecipients = 0;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;
}
