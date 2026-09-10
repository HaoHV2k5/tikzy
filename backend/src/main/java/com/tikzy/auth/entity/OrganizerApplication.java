package com.tikzy.auth.entity;

import com.tikzy.auth.enums.OrganizerApplicationStatus;
import com.tikzy.auth.enums.OrganizerType;
import com.tikzy.common.entity.BaseEntity;
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
@Table(name = "organizer_applications")
public class OrganizerApplication extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applicant_id", nullable = false)
    private User applicant;

    @Enumerated(EnumType.STRING)
    @Column(name = "organizer_type", nullable = false, length = 20)
    private OrganizerType organizerType;

    @Column(name = "organizer_name", nullable = false, length = 255)
    private String organizerName;

    @Column(name = "identity_number", length = 50)
    private String identityNumber;

    @Column(name = "tax_code", length = 50)
    private String taxCode;

    @Column(name = "contact_phone", nullable = false, length = 20)
    private String contactPhone;

    @Column(name = "address", nullable = false, length = 500)
    private String address;

    @Column(name = "website_url", length = 500)
    private String websiteUrl;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OrganizerApplicationStatus status = OrganizerApplicationStatus.PENDING;

    @Column(name = "review_note", length = 1000)
    private String reviewNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;
}
