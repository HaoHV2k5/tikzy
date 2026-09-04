-- =====================================================================
-- TIKZY - V10: Green event-ticketing email templates
-- =====================================================================

-- ---------------------------------------------------------------------
-- ACCOUNT_CREATED
-- ---------------------------------------------------------------------

UPDATE email_templates
SET
    html_content = $email$
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Chào mừng bạn đến với Tikzy</title>
</head>
<body style="margin:0; padding:0; background-color:#f0fdf4; color:#14281d; font-family:Arial, Helvetica, sans-serif; -webkit-text-size-adjust:100%; -ms-text-size-adjust:100%;">
    <div style="display:none; max-height:0; overflow:hidden; opacity:0; color:transparent;">Vé thành viên Tikzy của bạn đã sẵn sàng.</div>
    <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="width:100%; background-color:#f0fdf4;">
        <tr>
            <td align="center" style="padding:32px 16px;">
                <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="width:100%; max-width:600px; border-radius:24px; overflow:hidden;">
                    <!-- Header: poster-style gradient -->
                    <tr>
                        <td style="padding:36px 36px 30px; background-color:#064e3b; background:linear-gradient(135deg,#064e3b 0%,#059669 55%,#10b981 100%);" bgcolor="#064e3b">
                            <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0">
                                <tr>
                                    <td style="padding-bottom:24px;">
                                        <span style="color:#ffffff; font-size:24px; font-weight:800; letter-spacing:4px;">TIKZY</span>
                                        <span style="color:#bef264; font-size:10px; font-weight:700; letter-spacing:2px;">&middot; LET THE SHOW BEGIN</span>
                                    </td>
                                </tr>
                                <tr>
                                    <td>
                                        <span style="display:inline-block; padding:7px 12px; border:2px solid #bef264; border-radius:999px; color:#bef264; font-size:10px; font-weight:700; letter-spacing:1.5px;">THÀNH VIÊN MỚI</span>
                                        <h1 style="margin:16px 0 10px; color:#ffffff; font-size:36px; line-height:1.1; font-weight:800; letter-spacing:-1px;">Sẵn sàng bùng nổ<br>cùng <span style="color:#bef264;">Tikzy</span>!</h1>
                                        <p style="margin:0 0 16px; color:#d1fae5; font-size:15px; line-height:1.6;">Sân khấu, ánh đèn và những khoảnh khắc đáng nhớ đang chờ bạn.</p>
                                    </td>
                                </tr>
                                <tr>
                                    <td>
                                        <span style="display:inline-block; width:10px; height:10px; border-radius:50%; background-color:#bef264; margin-right:8px;"></span>
                                        <span style="display:inline-block; width:10px; height:10px; border-radius:50%; background-color:#ffffff; margin-right:8px;"></span>
                                        <span style="display:inline-block; width:10px; height:10px; border-radius:50%; background-color:#34d399; margin-right:8px;"></span>
                                        <span style="display:inline-block; width:10px; height:10px; border-radius:50%; background-color:#ffffff; margin-right:8px;"></span>
                                        <span style="display:inline-block; width:10px; height:10px; border-radius:50%; background-color:#bef264;"></span>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                    <!-- Body -->
                    <tr>
                        <td style="padding:36px; background-color:#ffffff;">
                            <p style="margin:0 0 16px; color:#14281d; font-size:17px; line-height:1.5;">Xin chào <strong>{{fullName}}</strong>,</p>
                            <p style="margin:0 0 28px; color:#4d6b5d; font-size:15px; line-height:1.7;">Tài khoản Tikzy của bạn đã được tạo thành công &mdash; cửa vào thế giới sự kiện chính thức mở!</p>

                            <!-- Member pass: ticket-stub card -->
                            <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="width:100%; margin-bottom:28px; border:2px solid #bbf7d0; border-radius:16px;">
                                <tr>
                                    <td style="padding:20px 18px 20px 18px; border-bottom:2px dashed #bbf7d0;">
                                        <p style="margin:0 0 6px; color:#6fae8b; font-size:10px; font-weight:700; letter-spacing:1.6px;">VÉ THÀNH VIÊN &middot; MEMBER PASS</p>
                                        <p style="margin:0; color:#14281d; font-size:16px; line-height:1.5; font-weight:700; word-break:break-word;">{{email}}</p>
                                    </td>
                                </tr>
                                <tr>
                                    <td style="padding:16px 18px 18px;">
                                        <span style="display:inline-block; width:3px; height:32px; background-color:#14281d; margin-right:3px;"></span>
                                        <span style="display:inline-block; width:2px; height:32px; background-color:#14281d; margin-right:3px;"></span>
                                        <span style="display:inline-block; width:4px; height:32px; background-color:#14281d; margin-right:3px;"></span>
                                        <span style="display:inline-block; width:2px; height:32px; background-color:#14281d; margin-right:3px;"></span>
                                        <span style="display:inline-block; width:3px; height:32px; background-color:#14281d; margin-right:3px;"></span>
                                        <span style="display:inline-block; width:4px; height:32px; background-color:#14281d;"></span>
                                        <p style="margin:10px 0 0; color:#6fae8b; font-size:10px; line-height:1.4; letter-spacing:1.6px;">TIKZY MEMBER &middot; 2026</p>
                                    </td>
                                </tr>
                            </table>

                            <!-- What you can do -->
                            <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="width:100%; margin-bottom:28px;">
                                <tr>
                                    <td width="36" valign="top" style="width:36px;">
                                        <span style="display:inline-block; width:26px; height:26px; border-radius:50%; background-color:#d1fae5; color:#059669; font-size:12px; line-height:26px; font-weight:800; text-align:center;">1</span>
                                    </td>
                                    <td valign="top" style="padding:0 0 14px 4px;">
                                        <p style="margin:0 0 2px; color:#14281d; font-size:14px; line-height:1.5; font-weight:700;">Đăng nhập vào Tikzy</p>
                                        <p style="margin:0; color:#7a9488; font-size:13px; line-height:1.5;">Dùng email trên để bắt đầu.</p>
                                    </td>
                                </tr>
                                <tr>
                                    <td width="36" valign="top" style="width:36px;">
                                        <span style="display:inline-block; width:26px; height:26px; border-radius:50%; background-color:#ecfccb; color:#65a30d; font-size:12px; line-height:26px; font-weight:800; text-align:center;">2</span>
                                    </td>
                                    <td valign="top" style="padding:0 0 14px 4px;">
                                        <p style="margin:0 0 2px; color:#14281d; font-size:14px; line-height:1.5; font-weight:700;">Săn sự kiện hot</p>
                                        <p style="margin:0; color:#7a9488; font-size:13px; line-height:1.5;">Âm nhạc, sân khấu, thể thao &mdash; chọn vibe của bạn.</p>
                                    </td>
                                </tr>
                                <tr>
                                    <td width="36" valign="top" style="width:36px;">
                                        <span style="display:inline-block; width:26px; height:26px; border-radius:50%; background-color:#ccfbf1; color:#0d9488; font-size:12px; line-height:26px; font-weight:800; text-align:center;">3</span>
                                    </td>
                                    <td valign="top" style="padding:0 0 0 4px;">
                                        <p style="margin:0 0 2px; color:#14281d; font-size:14px; line-height:1.5; font-weight:700;">Đặt vé trong vài giây</p>
                                        <p style="margin:0; color:#7a9488; font-size:13px; line-height:1.5;">Vé QR gửi ngay vào tài khoản của bạn.</p>
                                    </td>
                                </tr>
                            </table>

                            <p style="margin:0; padding-top:22px; border-top:1px solid #e8f5ec; color:#4d6b5d; font-size:14px; line-height:1.7;">Hẹn gặp bạn ở hàng ghế đầu!</p>
                            <p style="margin:20px 0 0; color:#4d6b5d; font-size:14px; line-height:1.7;">Trân trọng,<br><strong style="color:#14281d;">Đội ngũ Tikzy</strong></p>
                        </td>
                    </tr>
                    <!-- Footer -->
                    <tr>
                        <td style="padding:20px 36px; background-color:#e9f7ee;">
                            <p style="margin:0 0 4px; color:#7a9488; font-size:11px; line-height:1.6;">Email tự động từ hệ thống Tikzy, vui lòng không trả lời.</p>
                            <p style="margin:0; color:#9db8aa; font-size:11px; line-height:1.6;">Tikzy &middot; Nền tảng đặt vé sự kiện</p>
                        </td>
                    </tr>
                </table>
            </td>
        </tr>
    </table>
</body>
</html>
    $email$,
    text_content = $text$
Chào mừng bạn đến với Tikzy, {{fullName}}!

Tài khoản Tikzy với email {{email}} đã được tạo thành công.
Bạn có thể đăng nhập để khám phá và đặt vé cho những sự kiện yêu thích.

Hẹn gặp bạn ở hàng ghế đầu!
Trân trọng,
Đội ngũ Tikzy
    $text$
WHERE code = 'ACCOUNT_CREATED';

-- ---------------------------------------------------------------------
-- PASSWORD_RESET_OTP
-- ---------------------------------------------------------------------

UPDATE email_templates
SET
    html_content = $email$
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Mã OTP đặt lại mật khẩu Tikzy</title>
</head>
<body style="margin:0; padding:0; background-color:#f0fdf4; color:#14281d; font-family:Arial, Helvetica, sans-serif; -webkit-text-size-adjust:100%; -ms-text-size-adjust:100%;">
    <div style="display:none; max-height:0; overflow:hidden; opacity:0; color:transparent;">Mã OTP đặt lại mật khẩu Tikzy của bạn.</div>
    <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="width:100%; background-color:#f0fdf4;">
        <tr>
            <td align="center" style="padding:32px 16px;">
                <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="width:100%; max-width:600px; border-radius:24px; overflow:hidden;">
                    <tr>
                        <td style="padding:36px 36px 30px; background-color:#064e3b; background:linear-gradient(135deg,#064e3b 0%,#059669 55%,#10b981 100%);" bgcolor="#064e3b">
                            <p style="margin:0 0 24px; color:#ffffff; font-size:24px; font-weight:800; letter-spacing:4px;">TIKZY <span style="color:#bef264; font-size:10px; font-weight:700; letter-spacing:2px;">&middot; ACCOUNT SECURITY</span></p>
                            <span style="display:inline-block; padding:7px 12px; border:2px solid #bef264; border-radius:999px; color:#bef264; font-size:10px; font-weight:700; letter-spacing:1.5px;">ĐẶT LẠI MẬT KHẨU</span>
                            <h1 style="margin:16px 0 10px; color:#ffffff; font-size:36px; line-height:1.1; font-weight:800; letter-spacing:-1px;">Mã OTP<br>của bạn đây!</h1>
                            <p style="margin:0; color:#d1fae5; font-size:15px; line-height:1.6;">Xác thực nhanh rồi quay lại săn vé ngay.</p>
                        </td>
                    </tr>
                    <tr>
                        <td style="padding:36px; background-color:#ffffff;">
                            <p style="margin:0 0 16px; color:#14281d; font-size:17px; line-height:1.5;">Xin chào <strong>{{fullName}}</strong>,</p>
                            <p style="margin:0 0 28px; color:#4d6b5d; font-size:15px; line-height:1.7;">Chúng tôi nhận được yêu cầu đặt lại mật khẩu. Nhập mã OTP bên dưới để xác thực.</p>

                            <!-- OTP ticket -->
                            <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="width:100%; margin-bottom:24px; border:2px dashed #bbf7d0; border-radius:16px; background-color:#f6fdf9;">
                                <tr>
                                    <td align="center" style="padding:24px 16px 22px;">
                                        <p style="margin:0 0 10px; color:#6fae8b; font-size:10px; font-weight:700; letter-spacing:1.6px;">MÃ XÁC THỰC</p>
                                        <p style="margin:0; color:#059669; font-size:36px; line-height:1.25; font-weight:800; letter-spacing:12px;">{{otp}}</p>
                                        <p style="margin:12px 0 0; color:#7a9488; font-size:13px; line-height:1.6;">Có hiệu lực trong <strong style="color:#14281d;">{{expiresInMinutes}} phút</strong></p>
                                    </td>
                                </tr>
                            </table>

                            <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="width:100%; margin-bottom:28px; background-color:#f7fee7;">
                                <tr>
                                    <td width="4" style="width:4px; background-color:#84cc16;"></td>
                                    <td style="padding:14px 16px; color:#3f6212; font-size:13px; line-height:1.6;">Nếu bạn không yêu cầu đặt lại mật khẩu, hãy bỏ qua email. Đừng chia sẻ mã OTP với bất kỳ ai.</td>
                                </tr>
                            </table>

                            <p style="margin:0; padding-top:22px; border-top:1px solid #e8f5ec; color:#4d6b5d; font-size:14px; line-height:1.7;">Trân trọng,<br><strong style="color:#14281d;">Đội ngũ Tikzy</strong></p>
                        </td>
                    </tr>
                    <tr>
                        <td style="padding:20px 36px; background-color:#e9f7ee;">
                            <p style="margin:0 0 4px; color:#7a9488; font-size:11px; line-height:1.6;">Email tự động từ hệ thống Tikzy, vui lòng không trả lời.</p>
                            <p style="margin:0; color:#9db8aa; font-size:11px; line-height:1.6;">Tikzy &middot; Nền tảng đặt vé sự kiện</p>
                        </td>
                    </tr>
                </table>
            </td>
        </tr>
    </table>
</body>
</html>
    $email$,
    text_content = $text$
Yêu cầu đặt lại mật khẩu Tikzy

Xin chào {{fullName}},
Mã OTP của bạn là: {{otp}}
Mã có hiệu lực trong {{expiresInMinutes}} phút. Không chia sẻ mã này với bất kỳ ai.

Nếu bạn không thực hiện yêu cầu này, hãy bỏ qua email.
    $text$
WHERE code = 'PASSWORD_RESET_OTP';

-- ---------------------------------------------------------------------
-- ACCOUNT_UNLOCK_OTP
-- ---------------------------------------------------------------------

UPDATE email_templates
SET
    html_content = $email$
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Mã OTP mở khóa tài khoản Tikzy</title>
</head>
<body style="margin:0; padding:0; background-color:#f0fdf4; color:#14281d; font-family:Arial, Helvetica, sans-serif; -webkit-text-size-adjust:100%; -ms-text-size-adjust:100%;">
    <div style="display:none; max-height:0; overflow:hidden; opacity:0; color:transparent;">Mã OTP mở khóa tài khoản Tikzy của bạn.</div>
    <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="width:100%; background-color:#f0fdf4;">
        <tr>
            <td align="center" style="padding:32px 16px;">
                <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="width:100%; max-width:600px; border-radius:24px; overflow:hidden;">
                    <tr>
                        <td style="padding:36px 36px 30px; background-color:#064e3b; background:linear-gradient(135deg,#064e3b 0%,#059669 55%,#10b981 100%);" bgcolor="#064e3b">
                            <p style="margin:0 0 24px; color:#ffffff; font-size:24px; font-weight:800; letter-spacing:4px;">TIKZY <span style="color:#bef264; font-size:10px; font-weight:700; letter-spacing:2px;">&middot; ACCOUNT RECOVERY</span></p>
                            <span style="display:inline-block; padding:7px 12px; border:2px solid #bef264; border-radius:999px; color:#bef264; font-size:10px; font-weight:700; letter-spacing:1.5px;">MỞ KHÓA TÀI KHOẢN</span>
                            <h1 style="margin:16px 0 10px; color:#ffffff; font-size:36px; line-height:1.1; font-weight:800; letter-spacing:-1px;">Sẵn sàng<br>quay lại sân chơi!</h1>
                            <p style="margin:0; color:#d1fae5; font-size:15px; line-height:1.6;">Chỉ một bước xác thực nữa là bạn lại săn vé tiếp.</p>
                        </td>
                    </tr>
                    <tr>
                        <td style="padding:36px; background-color:#ffffff;">
                            <p style="margin:0 0 16px; color:#14281d; font-size:17px; line-height:1.5;">Xin chào <strong>{{fullName}}</strong>,</p>
                            <p style="margin:0 0 28px; color:#4d6b5d; font-size:15px; line-height:1.7;">Tài khoản Tikzy của bạn bị khóa do đăng nhập sai quá số lần cho phép. Dùng mã OTP bên dưới để khôi phục tài khoản.</p>

                            <!-- OTP ticket -->
                            <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="width:100%; margin-bottom:24px; border:2px dashed #bbf7d0; border-radius:16px; background-color:#f6fdf9;">
                                <tr>
                                    <td align="center" style="padding:24px 16px 22px;">
                                        <p style="margin:0 0 10px; color:#6fae8b; font-size:10px; font-weight:700; letter-spacing:1.6px;">MÃ XÁC THỰC</p>
                                        <p style="margin:0; color:#059669; font-size:36px; line-height:1.25; font-weight:800; letter-spacing:12px;">{{otp}}</p>
                                        <p style="margin:12px 0 0; color:#7a9488; font-size:13px; line-height:1.6;">Có hiệu lực trong <strong style="color:#14281d;">{{expiresInMinutes}} phút</strong></p>
                                    </td>
                                </tr>
                            </table>

                            <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="width:100%; margin-bottom:28px; background-color:#f7fee7;">
                                <tr>
                                    <td width="4" style="width:4px; background-color:#84cc16;"></td>
                                    <td style="padding:14px 16px; color:#3f6212; font-size:13px; line-height:1.6;">Nếu bạn không yêu cầu mở khóa, hãy bỏ qua email. Đừng chia sẻ mã OTP với bất kỳ ai.</td>
                                </tr>
                            </table>

                            <p style="margin:0; padding-top:22px; border-top:1px solid #e8f5ec; color:#4d6b5d; font-size:14px; line-height:1.7;">Trân trọng,<br><strong style="color:#14281d;">Đội ngũ Tikzy</strong></p>
                        </td>
                    </tr>
                    <tr>
                        <td style="padding:20px 36px; background-color:#e9f7ee;">
                            <p style="margin:0 0 4px; color:#7a9488; font-size:11px; line-height:1.6;">Email tự động từ hệ thống Tikzy, vui lòng không trả lời.</p>
                            <p style="margin:0; color:#9db8aa; font-size:11px; line-height:1.6;">Tikzy &middot; Nền tảng đặt vé sự kiện</p>
                        </td>
                    </tr>
                </table>
            </td>
        </tr>
    </table>
</body>
</html>
    $email$,
    text_content = $text$
Yêu cầu mở khóa tài khoản Tikzy

Xin chào {{fullName}},
Tài khoản Tikzy của bạn bị khóa do đăng nhập sai quá số lần cho phép.
Mã OTP để khôi phục tài khoản là: {{otp}}
Mã có hiệu lực trong {{expiresInMinutes}} phút. Không chia sẻ mã này với bất kỳ ai.

Nếu bạn không thực hiện yêu cầu này, hãy bỏ qua email.
    $text$
WHERE code = 'ACCOUNT_UNLOCK_OTP';
