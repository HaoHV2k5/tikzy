package com.tikzy.ticket.entity;

import com.tikzy.common.entity.BaseEntity;
import com.tikzy.event.entity.ShowTime;
import com.tikzy.event.entity.TicketType;
import com.tikzy.order.entity.Order;
import com.tikzy.ticket.enums.TicketStatus;
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

/**
 * Mỗi vé là 1 bản ghi riêng biệt với 1 mã QR độc lập
 * (mua 4 vé = 4 mã QR riêng, đi riêng cổng hoặc chia sẻ cho bạn bè).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tickets")
public class Ticket extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_type_id", nullable = false)
    private TicketType ticketType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "show_time_id", nullable = false)
    private ShowTime showTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    /**
     * Chuỗi JSON ký số HMAC-SHA256:
     * {ticketId, eventId, ticketType, seatNumber, customerName, signature}
     * — scanner gửi lên Backend để verify và check-in online.
     */
    @Column(name = "qr_payload", unique = true, columnDefinition = "TEXT")
    private String qrPayload;

    @Column(name = "seat_number", length = 20)
    private String seatNumber;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TicketStatus status = TicketStatus.AVAILABLE;
}
