-- =====================================================================
-- TIKZY - V7: Account unlock OTP and one-time password reset challenge
-- =====================================================================

CREATE TABLE account_unlock_requests (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id               UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    otp_hash              VARCHAR(255) NOT NULL,
    otp_attempts          INTEGER      NOT NULL DEFAULT 0,
    otp_expires_at        TIMESTAMP    NOT NULL,
    reset_token_hash      VARCHAR(64) UNIQUE,
    reset_token_expires_at TIMESTAMP,
    otp_verified_at       TIMESTAMP,
    consumed_at           TIMESTAMP,
    created_at            TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at            TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT chk_account_unlock_otp_attempts CHECK (otp_attempts >= 0)
);

CREATE INDEX idx_account_unlock_requests_user_id
    ON account_unlock_requests(user_id, created_at DESC);

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
    'ACCOUNT_UNLOCK_OTP',
    'OTP mở khóa tài khoản',
    'Mã OTP mở khóa tài khoản Tikzy',
    $email$
<!DOCTYPE html>
<html lang="vi">
<body style="font-family: Arial, sans-serif; color: #202124; line-height: 1.6;">
    <h2>Yêu cầu mở khóa tài khoản Tikzy</h2>
    <p>Xin chào {{fullName}},</p>
    <p>Tài khoản Tikzy của bạn đang bị khóa vì đăng nhập sai quá số lần cho phép.</p>
    <p>Mã OTP để tiếp tục khôi phục tài khoản là:</p>
    <p style="font-size: 28px; font-weight: bold; letter-spacing: 8px;">{{otp}}</p>
    <p>Mã có hiệu lực trong {{expiresInMinutes}} phút. Không chia sẻ mã này với bất kỳ ai.</p>
    <p>Nếu bạn không thực hiện yêu cầu này, hãy bỏ qua email.</p>
</body>
</html>
    $email$,
    $text$
Yêu cầu mở khóa tài khoản Tikzy

Xin chào {{fullName}},
Tài khoản Tikzy của bạn đang bị khóa vì đăng nhập sai quá số lần cho phép.
Mã OTP để tiếp tục khôi phục tài khoản là: {{otp}}
Mã có hiệu lực trong {{expiresInMinutes}} phút. Không chia sẻ mã này với bất kỳ ai.

Nếu bạn không thực hiện yêu cầu này, hãy bỏ qua email.
    $text$,
    TRUE
)
ON CONFLICT (code) DO NOTHING;
