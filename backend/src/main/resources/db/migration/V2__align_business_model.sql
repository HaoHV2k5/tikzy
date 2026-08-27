-- =====================================================================
-- TIKZY - V2: Align order/show-time, voucher ownership and payment safety
-- =====================================================================

-- ---------- ORDER / SHOW TIME INVENTORY ----------

ALTER TABLE orders
    ADD COLUMN show_time_id UUID;

ALTER TABLE orders
    ADD CONSTRAINT fk_orders_show_time
        FOREIGN KEY (show_time_id) REFERENCES show_times(id);

-- Existing orders must have one unambiguous show time before the column
-- becomes mandatory. The migration stops instead of guessing when data is
-- missing or an order contains tickets from multiple show times.
UPDATE orders o
SET show_time_id = t.show_time_id
FROM (
    SELECT order_id, MIN(show_time_id) AS show_time_id
    FROM tickets
    WHERE order_id IS NOT NULL
    GROUP BY order_id
    HAVING COUNT(DISTINCT show_time_id) = 1
) t
WHERE o.id = t.order_id
  AND o.show_time_id IS NULL;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM orders WHERE show_time_id IS NULL) THEN
        RAISE EXCEPTION 'Cannot enforce orders.show_time_id NOT NULL: some orders have no unambiguous show time';
    END IF;
END
$$;

ALTER TABLE orders
    ALTER COLUMN show_time_id SET NOT NULL;

CREATE INDEX idx_orders_show_time_id ON orders(show_time_id);

-- Inventory belongs to a concrete show time and ticket type. The old
-- ticket_types quantity columns are migrated before being removed because
-- they cannot represent independent inventory for multiple show times.
CREATE TABLE show_time_ticket_inventories (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    show_time_id    UUID    NOT NULL REFERENCES show_times(id) ON DELETE CASCADE,
    ticket_type_id  UUID    NOT NULL REFERENCES ticket_types(id) ON DELETE CASCADE,
    total_quantity  INTEGER NOT NULL,
    reserved_quantity INTEGER NOT NULL DEFAULT 0,
    sold_quantity   INTEGER NOT NULL DEFAULT 0,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uq_inventory_show_time_ticket_type
        UNIQUE (show_time_id, ticket_type_id),
    CONSTRAINT chk_inventory_total_quantity
        CHECK (total_quantity >= 0),
    CONSTRAINT chk_inventory_reserved_quantity
        CHECK (reserved_quantity >= 0),
    CONSTRAINT chk_inventory_sold_quantity
        CHECK (sold_quantity >= 0),
    CONSTRAINT chk_inventory_capacity
        CHECK (reserved_quantity + sold_quantity <= total_quantity)
);

-- For an existing V1 database, copy the event-level capacity to every show
-- time of that event. Existing issued tickets are used as the sold counter.
INSERT INTO show_time_ticket_inventories (
    show_time_id,
    ticket_type_id,
    total_quantity,
    sold_quantity
)
SELECT
    st.id,
    tt.id,
    tt.total_quantity,
    COUNT(t.id) FILTER (WHERE t.status IN ('SOLD', 'USED'))::INTEGER
FROM show_times st
JOIN ticket_types tt ON tt.event_id = st.event_id
LEFT JOIN tickets t
       ON t.show_time_id = st.id
      AND t.ticket_type_id = tt.id
GROUP BY st.id, tt.id, tt.total_quantity
ON CONFLICT (show_time_id, ticket_type_id) DO NOTHING;

ALTER TABLE ticket_types
    DROP CONSTRAINT IF EXISTS chk_ticket_types_quantity;

ALTER TABLE ticket_types
    DROP COLUMN IF EXISTS sold_quantity;

ALTER TABLE ticket_types
    DROP COLUMN IF EXISTS total_quantity;

CREATE INDEX idx_inventory_show_time_id
    ON show_time_ticket_inventories(show_time_id);

CREATE INDEX idx_inventory_ticket_type_id
    ON show_time_ticket_inventories(ticket_type_id);

-- ---------- PROMOTION OWNERSHIP / USAGE ----------

ALTER TABLE promotions
    ADD COLUMN max_usage_per_user INTEGER;

ALTER TABLE promotions
    ADD CONSTRAINT chk_promotions_max_usage_per_user
        CHECK (max_usage_per_user IS NULL OR max_usage_per_user > 0);

CREATE TABLE user_promotions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    promotion_id    UUID NOT NULL REFERENCES promotions(id),
    source          VARCHAR(30) NOT NULL,
    source_broadcast_id UUID REFERENCES event_broadcasts(id),
    status          VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    issued_at       TIMESTAMP NOT NULL DEFAULT now(),
    used_at         TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uq_user_promotion UNIQUE (user_id, promotion_id),
    CONSTRAINT chk_user_promotion_status
        CHECK (status IN ('AVAILABLE', 'USED', 'EXPIRED', 'REVOKED'))
);

CREATE INDEX idx_user_promotions_user_id
    ON user_promotions(user_id);

CREATE INDEX idx_user_promotions_promotion_id
    ON user_promotions(promotion_id);

CREATE TABLE promotion_usages (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    promotion_id    UUID NOT NULL REFERENCES promotions(id),
    user_id         UUID NOT NULL REFERENCES users(id),
    order_id        UUID NOT NULL REFERENCES orders(id),
    discount_amount NUMERIC(15, 2) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'RESERVED',
    used_at         TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uq_promotion_usage_order UNIQUE (order_id),
    CONSTRAINT chk_promotion_usage_discount CHECK (discount_amount >= 0),
    CONSTRAINT chk_promotion_usage_status
        CHECK (status IN ('RESERVED', 'COMPLETED', 'RELEASED'))
);

CREATE INDEX idx_promotion_usages_user_id
    ON promotion_usages(user_id);

CREATE INDEX idx_promotion_usages_promotion_id
    ON promotion_usages(promotion_id);

-- ---------- PAYMENT / REFUND IDEMPOTENCY ----------

-- Gateway transaction IDs are unique within a provider. NULL is allowed for
-- payment attempts that have not received a gateway transaction yet.
CREATE UNIQUE INDEX uq_payments_method_transaction_id
    ON payments(method, transaction_id)
    WHERE transaction_id IS NOT NULL;

-- An order may have multiple attempts, but only one settled payment.
CREATE UNIQUE INDEX uq_payments_one_settled_payment_per_order
    ON payments(order_id)
    WHERE status IN ('SUCCESS', 'REFUNDED');

ALTER TABLE refund_logs
    ADD COLUMN payment_id UUID;

ALTER TABLE refund_logs
    ADD COLUMN idempotency_key UUID NOT NULL DEFAULT gen_random_uuid();

-- Existing refund records, if any, must point to their successful payment.
UPDATE refund_logs r
SET payment_id = p.id
FROM payments p
WHERE p.order_id = r.order_id
  AND p.status IN ('SUCCESS', 'REFUNDED')
  AND r.payment_id IS NULL;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM refund_logs WHERE payment_id IS NULL) THEN
        RAISE EXCEPTION 'Cannot enforce refund_logs.payment_id NOT NULL: some refund logs have no successful payment';
    END IF;
END
$$;

ALTER TABLE refund_logs
    ALTER COLUMN payment_id SET NOT NULL;

ALTER TABLE refund_logs
    ADD CONSTRAINT fk_refund_logs_payment
        FOREIGN KEY (payment_id) REFERENCES payments(id);

ALTER TABLE refund_logs
    ADD CONSTRAINT uq_refund_logs_idempotency_key
        UNIQUE (idempotency_key);

CREATE UNIQUE INDEX uq_refund_logs_provider_refund_id
    ON refund_logs(payment_method, gateway_refund_id)
    WHERE gateway_refund_id IS NOT NULL;

CREATE INDEX idx_refund_logs_payment_id
    ON refund_logs(payment_id);
