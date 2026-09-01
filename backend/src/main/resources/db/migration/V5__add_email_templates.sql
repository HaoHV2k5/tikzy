-- =====================================================================
-- TIKZY - V5: Reusable transactional email templates
-- =====================================================================

CREATE TABLE email_templates (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code         VARCHAR(100) NOT NULL UNIQUE,
    name         VARCHAR(255) NOT NULL,
    subject      VARCHAR(255) NOT NULL,
    html_content TEXT         NOT NULL,
    text_content TEXT,
    is_active    BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at   TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX idx_email_templates_active
    ON email_templates(code, is_active);

INSERT INTO email_templates (
    code,
    name,
    subject,
    html_content,
    text_content,
    is_active
)
VALUES
(
    'ACCOUNT_CREATED',
    'Thông báo tạo tài khoản',
    'Chào mừng bạn đến với Tikzy, {{fullName}}!',
    $email$
<!DOCTYPE html>
<html lang="vi">
<body style="font-family: Arial, sans-serif; color: #202124; line-height: 1.6;">
    <h2>Chào mừng {{fullName}} đến với Tikzy!</h2>
    <p>Tài khoản Tikzy với email <strong>{{email}}</strong> đã được tạo thành công.</p>
    <p>Bạn có thể đăng nhập để khám phá và đặt vé cho những sự kiện yêu thích.</p>
    <p>Trân trọng,<br>Đội ngũ Tikzy</p>
</body>
</html>
    $email$,
    $text$
Chào mừng {{fullName}} đến với Tikzy!

Tài khoản Tikzy với email {{email}} đã được tạo thành công.
Bạn có thể đăng nhập để khám phá và đặt vé cho những sự kiện yêu thích.

Trân trọng,
Đội ngũ Tikzy
    $text$,
    TRUE
),
(
    'PASSWORD_RESET_OTP',
    'OTP đặt lại mật khẩu',
    'Mã OTP đặt lại mật khẩu Tikzy',
    $email$
<!DOCTYPE html>
<html lang="vi">
<body style="font-family: Arial, sans-serif; color: #202124; line-height: 1.6;">
    <h2>Yêu cầu đặt lại mật khẩu</h2>
    <p>Xin chào {{fullName}},</p>
    <p>Mã OTP của bạn là:</p>
    <p style="font-size: 28px; font-weight: bold; letter-spacing: 8px;">{{otp}}</p>
    <p>Mã có hiệu lực trong {{expiresInMinutes}} phút. Không chia sẻ mã này với bất kỳ ai.</p>
    <p>Nếu bạn không thực hiện yêu cầu này, hãy bỏ qua email.</p>
</body>
</html>
    $email$,
    $text$
Yêu cầu đặt lại mật khẩu

Xin chào {{fullName}},
Mã OTP của bạn là: {{otp}}
Mã có hiệu lực trong {{expiresInMinutes}} phút. Không chia sẻ mã này với bất kỳ ai.

Nếu bạn không thực hiện yêu cầu này, hãy bỏ qua email.
    $text$,
    TRUE
)
ON CONFLICT (code) DO NOTHING;
