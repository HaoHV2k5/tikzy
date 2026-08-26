package com.tikzy.advertisement.entity;

import com.tikzy.advertisement.enums.BannerPosition;
import com.tikzy.common.entity.BaseEntity;
import com.tikzy.event.entity.Event;
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

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "banners")
public class Banner extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ad_campaign_id") // nullable: NULL nếu Admin tự tạo
    private AdCampaign adCampaign;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id") // nullable: liên kết show hoặc dùng target_url ngoài
    private Event event;

    @Column(name = "title")
    private String title;

    @Column(name = "image_url", nullable = false)
    private String imageUrl; // Cloudinary CDN đang hiển thị

    @Column(name = "pending_image_url")
    private String pendingImageUrl; // ảnh mới upload đang chờ duyệt

    @Column(name = "target_url")
    private String targetUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "position", nullable = false, length = 30)
    private BannerPosition position;

    @Builder.Default
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}
