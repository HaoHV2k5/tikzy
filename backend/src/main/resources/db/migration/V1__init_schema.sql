-- =====================================================================
-- TIKZY — V1: Init Schema (Supabase PostgreSQL)
-- Quy chuẩn: toàn bộ PK dùng UUID với DEFAULT gen_random_uuid()
-- =====================================================================

-- ---------- AUTH & RBAC ----------

CREATE TABLE roles (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code        VARCHAR(50)  NOT NULL UNIQUE,  -- ROLE_CUSTOMER, ROLE_ORGANIZER, ROLE_ADMIN
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    created_at  TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE TABLE users (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    role_id       UUID         NOT NULL REFERENCES roles(id),
    email         VARCHAR(255) NOT NULL UNIQUE,
    phone         VARCHAR(20)  UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name     VARCHAR(255) NOT NULL,
    avatar_url    VARCHAR(500),                  -- Cloudinary
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE TABLE refresh_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token       VARCHAR(512) NOT NULL UNIQUE,
    device_info VARCHAR(500),
    ip_address  VARCHAR(45),
    expires_at  TIMESTAMP    NOT NULL,
    is_revoked  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);

-- ---------- EVENT MODULE ----------

CREATE TABLE categories (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name       VARCHAR(100) NOT NULL,           -- Âm nhạc, Sân khấu, Thể thao...
    slug       VARCHAR(100) NOT NULL UNIQUE,
    icon_url   VARCHAR(500),                    -- Cloudinary
    sort_order INTEGER      NOT NULL DEFAULT 0,
    is_active  BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE TABLE events (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organizer_id           UUID         NOT NULL REFERENCES users(id),
    category_id            UUID         NOT NULL REFERENCES categories(id),
    title                  VARCHAR(500) NOT NULL,
    description            TEXT,
    venue_name             VARCHAR(255),
    venue_address          VARCHAR(500),
    banner_url             VARCHAR(500),        -- Cloudinary
    thumbnail_url          VARCHAR(500),        -- Cloudinary
    status                 VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',   -- DRAFT, PENDING, APPROVED, PUBLISHED, CANCELLED, ENDED
    refund_policy          VARCHAR(20)  NOT NULL DEFAULT 'NO_REFUND', -- NO_REFUND, ALLOW_REFUND
    refund_deadline_days   INTEGER,
    refund_fee_percentage  NUMERIC(5, 2),
    cancellation_reason    TEXT,
    cancelled_at           TIMESTAMP,
    created_at             TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX idx_events_organizer_id ON events(organizer_id);
CREATE INDEX idx_events_category_id ON events(category_id);
CREATE INDEX idx_events_status ON events(status);

CREATE TABLE show_times (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id   UUID      NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    start_time TIMESTAMP NOT NULL,
    end_time   TIMESTAMP NOT NULL,
    is_active  BOOLEAN   NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_show_times_event_id ON show_times(event_id);

CREATE TABLE ticket_types (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id       UUID           NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    name           VARCHAR(100)   NOT NULL,  -- GA, VIP, VVIP, Early Bird...
    price          NUMERIC(15, 2) NOT NULL,
    total_quantity INTEGER        NOT NULL,
    sold_quantity  INTEGER        NOT NULL DEFAULT 0,
    max_per_order  INTEGER        NOT NULL DEFAULT 10,
    is_active      BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP      NOT NULL DEFAULT now(),
    -- Lớp chặn overselling thứ 3: DB constraint
    CONSTRAINT chk_ticket_types_quantity CHECK (sold_quantity <= total_quantity),
    CONSTRAINT chk_ticket_types_price CHECK (price >= 0)
);

CREATE INDEX idx_ticket_types_event_id ON ticket_types(event_id);

-- ---------- ADVERTISEMENT MODULE ----------

CREATE TABLE ad_packages (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code           VARCHAR(50)    NOT NULL UNIQUE,  -- HERO_SLIDER_7D, CATEGORY_TOP_3D
    name           VARCHAR(255)   NOT NULL,
    description    TEXT,
    price          NUMERIC(15, 2) NOT NULL,
    duration_days  INTEGER        NOT NULL,
    placement_type VARCHAR(30)    NOT NULL,          -- HOME_HERO_SLIDER, CATEGORY_TOP, BADGE_HOT
    is_active      BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP      NOT NULL DEFAULT now()
);

CREATE TABLE ad_campaigns (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    package_id     UUID           NOT NULL REFERENCES ad_packages(id),
    event_id       UUID           NOT NULL REFERENCES events(id),
    organizer_id   UUID           NOT NULL REFERENCES users(id),
    total_price    NUMERIC(15, 2) NOT NULL,
    billing_type   VARCHAR(20)    NOT NULL,          -- PREPAID, POSTPAID_ESCROW
    payment_method VARCHAR(20),                      -- VNPAY, MOMO, DEDUCT_ESCROW
    payment_status VARCHAR(20)    NOT NULL DEFAULT 'UNPAID',  -- UNPAID, PAID, WAITING_SETTLEMENT, SETTLED, CANCELLED, REFUNDED
    status         VARCHAR(20)    NOT NULL DEFAULT 'PENDING_APPROVAL', -- PENDING_APPROVAL, APPROVED, RUNNING, COMPLETED, REJECTED
    start_date     TIMESTAMP,
    end_date       TIMESTAMP,
    note           TEXT,
    created_at     TIMESTAMP      NOT NULL DEFAULT now()
);

CREATE INDEX idx_ad_campaigns_event_id ON ad_campaigns(event_id);
CREATE INDEX idx_ad_campaigns_organizer_id ON ad_campaigns(organizer_id);

CREATE TABLE banners (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ad_campaign_id     UUID REFERENCES ad_campaigns(id),  -- NULL nếu Admin tự tạo
    event_id           UUID REFERENCES events(id),        -- NULL nếu dùng target_url ngoài
    title              VARCHAR(255),
    image_url          VARCHAR(500) NOT NULL,             -- Cloudinary CDN đang hiển thị
    pending_image_url  VARCHAR(500),                      -- ảnh mới chờ duyệt đổi
    target_url         VARCHAR(500),
    position           VARCHAR(30)  NOT NULL,             -- HOME_HERO_SLIDER, HOME_SUB_BANNER, EVENT_SIDEBAR
    sort_order         INTEGER      NOT NULL DEFAULT 0,
    start_date         TIMESTAMP,
    end_date           TIMESTAMP,
    is_active          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at         TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX idx_banners_position_active ON banners(position, is_active);

-- ---------- PROMOTION MODULE ----------

CREATE TABLE promotions (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id            UUID REFERENCES events(id),       -- NULL: áp dụng toàn sàn
    organizer_id        UUID REFERENCES users(id),
    code                VARCHAR(50)    NOT NULL UNIQUE,   -- SUMMER2026, FANLOVE25
    discount_type       VARCHAR(20)    NOT NULL,          -- PERCENTAGE, FIXED_AMOUNT
    discount_value      NUMERIC(15, 2) NOT NULL,
    min_order_amount    NUMERIC(15, 2),
    max_discount_amount NUMERIC(15, 2),
    total_usage_limit   INTEGER,
    used_count          INTEGER        NOT NULL DEFAULT 0,
    is_compensation     BOOLEAN        NOT NULL DEFAULT FALSE, -- voucher đền bù show hủy
    valid_from          TIMESTAMP,
    valid_to            TIMESTAMP,
    is_active           BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP      NOT NULL DEFAULT now()
);

CREATE INDEX idx_promotions_event_id ON promotions(event_id);

-- ---------- ORDER MODULE ----------

CREATE TABLE orders (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_code      VARCHAR(30)    NOT NULL UNIQUE,       -- TKZ-YYYYMMDD-XXXXX
    user_id         UUID           NOT NULL REFERENCES users(id),
    event_id        UUID           NOT NULL REFERENCES events(id),
    promotion_id    UUID REFERENCES promotions(id),
    subtotal        NUMERIC(15, 2) NOT NULL,              -- giá gốc trước giảm
    discount_amount NUMERIC(15, 2) NOT NULL DEFAULT 0,
    total_amount    NUMERIC(15, 2) NOT NULL,              -- tiền THỰC TRẢ (dùng để refund)
    status          VARCHAR(20)    NOT NULL DEFAULT 'PENDING', -- PENDING, PAID, CANCELLED, REFUNDED, EXPIRED
    customer_name   VARCHAR(255)   NOT NULL,
    customer_email  VARCHAR(255)   NOT NULL,
    customer_phone  VARCHAR(20),
    expires_at      TIMESTAMP      NOT NULL,              -- giữ chỗ 15 phút
    created_at      TIMESTAMP      NOT NULL DEFAULT now()
);

CREATE INDEX idx_orders_user_id ON orders(user_id);
CREATE INDEX idx_orders_event_id ON orders(event_id);
CREATE INDEX idx_orders_status_expires ON orders(status, expires_at);

CREATE TABLE order_items (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id       UUID           NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    ticket_type_id UUID           NOT NULL REFERENCES ticket_types(id),
    quantity       INTEGER        NOT NULL,
    unit_price     NUMERIC(15, 2) NOT NULL,
    created_at     TIMESTAMP      NOT NULL DEFAULT now()
);

CREATE INDEX idx_order_items_order_id ON order_items(order_id);

-- ---------- PAYMENT & REFUND MODULE ----------

-- 1 đơn có thể thử thanh toán nhiều lần (đổi cổng trong 15 phút giữ chỗ)
CREATE TABLE payments (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id       UUID           NOT NULL REFERENCES orders(id),
    method         VARCHAR(20)    NOT NULL,               -- VNPAY, MOMO, ZALOPAY
    transaction_id VARCHAR(255),                          -- mã GD từ cổng TT
    amount         NUMERIC(15, 2) NOT NULL,               -- = orders.total_amount
    status         VARCHAR(20)    NOT NULL DEFAULT 'PENDING', -- PENDING, SUCCESS, FAILED, CANCELLED, REFUNDED
    paid_at        TIMESTAMP,
    created_at     TIMESTAMP      NOT NULL DEFAULT now()
);

CREATE INDEX idx_payments_order_id ON payments(order_id);
CREATE INDEX idx_payments_transaction_id ON payments(transaction_id);
CREATE INDEX idx_payments_status_created ON payments(status, created_at);

CREATE TABLE refund_logs (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id            UUID           NOT NULL REFERENCES orders(id),
    event_id            UUID           NOT NULL REFERENCES events(id),
    amount              NUMERIC(15, 2) NOT NULL,          -- = orders.total_amount
    payment_method      VARCHAR(20),
    status              VARCHAR(20)    NOT NULL DEFAULT 'PROCESSING', -- PROCESSING, SUCCESS, FAILED, MANUAL_BANK
    gateway_refund_id   VARCHAR(255),
    refund_reason       VARCHAR(500),
    error_message       TEXT,
    bank_account_number VARCHAR(50),                      -- dự phòng chuyển tay
    bank_name           VARCHAR(100),
    bank_account_holder VARCHAR(255),
    completed_at        TIMESTAMP,
    created_at          TIMESTAMP      NOT NULL DEFAULT now()
);

CREATE INDEX idx_refund_logs_order_id ON refund_logs(order_id);
CREATE INDEX idx_refund_logs_event_id ON refund_logs(event_id);
CREATE INDEX idx_refund_logs_status ON refund_logs(status);

-- ---------- BROADCAST MODULE ----------

CREATE TABLE event_broadcasts (
    id                                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id                           UUID         NOT NULL REFERENCES events(id),
    organizer_id                       UUID         NOT NULL REFERENCES users(id),
    target_audience                    VARCHAR(30)  NOT NULL, -- ALL_ATTENDEES, VOUCHER_USERS_ONLY
    title                              VARCHAR(500) NOT NULL,
    message_content                    TEXT         NOT NULL,
    attached_compensation_promotion_id UUID REFERENCES promotions(id),
    total_recipients                   INTEGER      NOT NULL DEFAULT 0,
    sent_at                            TIMESTAMP,
    created_at                         TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX idx_event_broadcasts_event_id ON event_broadcasts(event_id);

-- ---------- SETTLEMENT MODULE (Escrow) ----------

CREATE TABLE settlements (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id                 UUID           NOT NULL REFERENCES events(id),
    organizer_id             UUID           NOT NULL REFERENCES users(id),
    total_gross_revenue      NUMERIC(18, 2) NOT NULL, -- tổng tiền bán vé thu hộ thực tế
    total_refunded_amount    NUMERIC(18, 2) NOT NULL DEFAULT 0,
    platform_commission_rate NUMERIC(5, 2)  NOT NULL, -- % phí Tikzy (vd: 8.00)
    platform_commission_fee  NUMERIC(18, 2) NOT NULL, -- gross * rate%
    total_ad_fee_deducted    NUMERIC(18, 2) NOT NULL DEFAULT 0, -- gói quảng cáo POSTPAID_ESCROW
    net_payout_amount        NUMERIC(18, 2) NOT NULL, -- gross - refund - commission - ad_fee
    status                   VARCHAR(20)    NOT NULL DEFAULT 'PENDING', -- PENDING, APPROVED, PAID, CANCELLED
    bank_name                VARCHAR(100),
    bank_account_number      VARCHAR(50),
    bank_account_holder      VARCHAR(255),
    bank_transfer_reference  VARCHAR(100),            -- mã UNC chuyển khoản
    settled_at               TIMESTAMP,
    created_at               TIMESTAMP      NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX idx_settlements_event_id ON settlements(event_id);

-- ---------- TICKET & CHECK-IN MODULE ----------

CREATE TABLE tickets (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_type_id UUID      NOT NULL REFERENCES ticket_types(id),
    show_time_id   UUID      NOT NULL REFERENCES show_times(id),
    order_id       UUID REFERENCES orders(id),
    qr_payload     TEXT      UNIQUE,                      -- JSON ký số HMAC-SHA256
    seat_number    VARCHAR(20),
    status         VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE', -- AVAILABLE, SOLD, USED, CANCELLED
    created_at     TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_tickets_order_id ON tickets(order_id);
CREATE INDEX idx_tickets_show_time_id ON tickets(show_time_id);

CREATE TABLE check_ins (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_id     UUID      NOT NULL UNIQUE REFERENCES tickets(id), -- chống double check-in
    staff_id      UUID      NOT NULL REFERENCES users(id),
    method        VARCHAR(20) NOT NULL DEFAULT 'QR_SCAN',           -- QR_SCAN, MANUAL
    checked_in_at TIMESTAMP   NOT NULL DEFAULT now(),
    created_at    TIMESTAMP   NOT NULL DEFAULT now()
);

-- ---------- SEED DATA: ROLES ----------

INSERT INTO roles (code, name, description) VALUES
    ('ROLE_CUSTOMER',  'Khách hàng',       'Người mua vé'),
    ('ROLE_ORGANIZER', 'Ban tổ chức',      'Nhà tổ chức sự kiện (BTC)'),
    ('ROLE_ADMIN',     'Quản trị viên',    'Admin Tikzy');
