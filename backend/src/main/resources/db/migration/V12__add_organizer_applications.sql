-- Organizer onboarding requires an admin-reviewed application before a
-- customer account receives ROLE_ORGANIZER.
CREATE TABLE organizer_applications (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    applicant_id    UUID         NOT NULL REFERENCES users(id),
    organizer_type  VARCHAR(20)  NOT NULL,
    organizer_name  VARCHAR(255) NOT NULL,
    identity_number VARCHAR(50),
    tax_code        VARCHAR(50),
    contact_phone   VARCHAR(20)  NOT NULL,
    address         VARCHAR(500) NOT NULL,
    website_url     VARCHAR(500),
    description     TEXT,
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    review_note     VARCHAR(1000),
    reviewed_by     UUID REFERENCES users(id),
    reviewed_at     TIMESTAMP,
    created_at      TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT chk_organizer_application_type
        CHECK (organizer_type IN ('INDIVIDUAL', 'BUSINESS')),
    CONSTRAINT chk_organizer_application_status
        CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    CONSTRAINT chk_organizer_application_document
        CHECK (
            (organizer_type = 'INDIVIDUAL' AND identity_number IS NOT NULL)
            OR (organizer_type = 'BUSINESS' AND tax_code IS NOT NULL)
        ),
    CONSTRAINT chk_organizer_application_review
        CHECK (
            (status = 'PENDING' AND reviewed_by IS NULL AND reviewed_at IS NULL)
            OR (status IN ('APPROVED', 'REJECTED') AND reviewed_by IS NOT NULL AND reviewed_at IS NOT NULL)
        ),
    CONSTRAINT chk_organizer_application_rejection_note
        CHECK (status <> 'REJECTED' OR review_note IS NOT NULL)
);

CREATE INDEX idx_organizer_applications_applicant_id
    ON organizer_applications(applicant_id);

CREATE INDEX idx_organizer_applications_status_created_at
    ON organizer_applications(status, created_at);

CREATE UNIQUE INDEX uq_organizer_applications_one_pending_per_user
    ON organizer_applications(applicant_id)
    WHERE status = 'PENDING';

CREATE UNIQUE INDEX uq_organizer_applications_active_identity
    ON organizer_applications(identity_number)
    WHERE identity_number IS NOT NULL
      AND status IN ('PENDING', 'APPROVED');

CREATE UNIQUE INDEX uq_organizer_applications_active_tax_code
    ON organizer_applications(tax_code)
    WHERE tax_code IS NOT NULL
      AND status IN ('PENDING', 'APPROVED');
