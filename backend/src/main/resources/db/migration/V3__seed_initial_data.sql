-- =====================================================================
-- TIKZY - V3: Seed initial admin, categories and advertising packages
-- =====================================================================

DO $$
BEGIN
    IF NULLIF('${admin_email}', '') IS NULL THEN
        RAISE EXCEPTION 'ADMIN_EMAIL must be configured before running V3';
    END IF;

    IF NULLIF('${admin_password_hash}', '') IS NULL THEN
        RAISE EXCEPTION 'ADMIN_PASSWORD_HASH must be configured before running V3';
    END IF;
END
$$;

-- ---------- CATEGORIES ----------

INSERT INTO categories (name, slug, sort_order, is_active)
VALUES
    ('Âm nhạc', 'am-nhac', 1, TRUE),
    ('Sân khấu', 'san-khau', 2, TRUE),
    ('Thể thao', 'the-thao', 3, TRUE),
    ('Hội thảo', 'hoi-thao', 4, TRUE),
    ('Lễ hội', 'le-hoi', 5, TRUE),
    ('Gia đình', 'gia-dinh', 6, TRUE)
ON CONFLICT (slug) DO NOTHING;

-- ---------- ADVERTISING PACKAGES ----------

INSERT INTO ad_packages (
    code,
    name,
    description,
    price,
    duration_days,
    placement_type,
    is_active
)
VALUES
    (
        'HERO_SLIDER_7D',
        'Hero Slider 7 ngày',
        'Hiển thị banner nổi bật tại khu vực hero trang chủ trong 7 ngày.',
        5000000.00,
        7,
        'HOME_HERO_SLIDER',
        TRUE
    ),
    (
        'CATEGORY_TOP_3D',
        'Ghim đầu danh mục 3 ngày',
        'Đưa sự kiện lên vị trí nổi bật đầu danh mục trong 3 ngày.',
        2000000.00,
        3,
        'CATEGORY_TOP',
        TRUE
    ),
    (
        'BADGE_HOT_7D',
        'Nhãn HOT 7 ngày',
        'Hiển thị nhãn HOT cho sự kiện trong 7 ngày.',
        500000.00,
        7,
        'BADGE_HOT',
        TRUE
    )
ON CONFLICT (code) DO NOTHING;

-- ---------- INITIAL ADMIN ----------

INSERT INTO users (role_id, email, password_hash, full_name, is_active)
SELECT
    r.id,
    '${admin_email}',
    '${admin_password_hash}',
    'Tikzy Administrator',
    TRUE
FROM roles r
WHERE r.code = 'ROLE_ADMIN'
ON CONFLICT (email) DO NOTHING;
