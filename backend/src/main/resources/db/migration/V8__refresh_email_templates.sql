-- =====================================================================
-- TIKZY - V8: Refresh transactional email templates
-- =====================================================================

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
<body style="margin:0; padding:0; background-color:#f3f6fb; color:#172033; font-family:Arial, Helvetica, sans-serif; -webkit-text-size-adjust:100%; -ms-text-size-adjust:100%;">
    <div style="display:none; max-height:0; overflow:hidden; opacity:0; color:transparent;">
        Tài khoản Tikzy của bạn đã sẵn sàng. Bắt đầu khám phá những sự kiện yêu thích.
    </div>
    <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="width:100%; background-color:#f3f6fb;">
        <tr>
            <td align="center" style="padding:32px 16px;">
                <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="width:100%; max-width:620px; border-radius:20px; overflow:hidden;">
                    <tr>
                        <td style="padding:34px 40px 38px; background-color:#101828; border-top:4px solid #ff6b4a;">
                            <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0">
                                <tr>
                                    <td style="padding-bottom:30px;">
                                        <span style="color:#ffffff; font-size:22px; font-weight:800; letter-spacing:3px;">TIKZY</span>
                                        <span style="color:#94a3b8; font-size:10px; font-weight:700; letter-spacing:1.5px;"> &middot; EVENT TICKETING</span>
                                    </td>
                                </tr>
                                <tr>
                                    <td>
                                        <span style="display:inline-block; padding:7px 11px; border:1px solid #344054; border-radius:999px; color:#ffab98; font-size:10px; font-weight:700; letter-spacing:1.2px;">TÀI KHOẢN MỚI</span>
                                        <h1 style="margin:18px 0 12px; color:#ffffff; font-size:34px; line-height:1.15; font-weight:800; letter-spacing:-0.8px;">Chào mừng bạn<br>đến với Tikzy!</h1>
                                        <p style="margin:0; color:#c6d0df; font-size:15px; line-height:1.7;">Một thế giới sự kiện đáng nhớ đang chờ bạn khám phá.</p>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                    <tr>
                        <td style="padding:40px; background-color:#ffffff;">
                            <p style="margin:0 0 18px; color:#172033; font-size:16px; line-height:1.6;">Xin chào <strong>{{fullName}}</strong>,</p>
                            <p style="margin:0 0 28px; color:#526078; font-size:15px; line-height:1.75;">Tài khoản Tikzy của bạn đã được tạo thành công. Từ bây giờ, bạn có thể tìm kiếm sự kiện, đặt vé và lưu lại những khoảnh khắc thật đáng nhớ.</p>

                            <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="width:100%; margin-bottom:28px; border:1px solid #e4eaf2; border-left:4px solid #ff6b4a; background-color:#fbfcfe;">
                                <tr>
                                    <td style="padding:19px 20px 18px;">
                                        <p style="margin:0 0 7px; color:#8a96a9; font-size:10px; font-weight:700; letter-spacing:1.4px;">EMAIL ĐĂNG KÝ</p>
                                        <p style="margin:0; color:#172033; font-size:16px; line-height:1.5; font-weight:700; word-break:break-word;">{{email}}</p>
                                    </td>
                                </tr>
                            </table>

                            <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="width:100%; margin-bottom:28px;">
                                <tr>
                                    <td width="28" valign="top" style="width:28px; padding-top:2px;">
                                        <span style="display:inline-block; width:22px; height:22px; border-radius:50%; background-color:#fff0ec; color:#ff6b4a; font-size:12px; line-height:22px; font-weight:800; text-align:center;">1</span>
                                    </td>
                                    <td valign="top" style="padding:0 0 15px 10px;">
                                        <p style="margin:0 0 3px; color:#172033; font-size:14px; line-height:1.5; font-weight:700;">Đăng nhập vào Tikzy</p>
                                        <p style="margin:0; color:#69768c; font-size:13px; line-height:1.6;">Sử dụng email trên để bắt đầu hành trình của bạn.</p>
                                    </td>
                                </tr>
                                <tr>
                                    <td width="28" valign="top" style="width:28px; padding-top:2px;">
                                        <span style="display:inline-block; width:22px; height:22px; border-radius:50%; background-color:#e7f8f5; color:#129b8b; font-size:12px; line-height:22px; font-weight:800; text-align:center;">2</span>
                                    </td>
                                    <td valign="top" style="padding:0 0 0 10px;">
                                        <p style="margin:0 0 3px; color:#172033; font-size:14px; line-height:1.5; font-weight:700;">Khám phá và đặt vé</p>
                                        <p style="margin:0; color:#69768c; font-size:13px; line-height:1.6;">Chọn sự kiện phù hợp và sẵn sàng cho trải nghiệm tiếp theo.</p>
                                    </td>
                                </tr>
                            </table>

                            <p style="margin:0; padding-top:24px; border-top:1px solid #edf1f6; color:#526078; font-size:14px; line-height:1.7;">Chúc bạn có những trải nghiệm tuyệt vời cùng Tikzy!</p>
                            <p style="margin:24px 0 0; color:#526078; font-size:14px; line-height:1.7;">Trân trọng,<br><strong style="color:#172033;">Đội ngũ Tikzy</strong></p>
                        </td>
                    </tr>
                    <tr>
                        <td style="padding:22px 40px; background-color:#f8fafc; border-top:1px solid #e9eef5;">
                            <p style="margin:0 0 6px; color:#8490a3; font-size:11px; line-height:1.6;">Đây là email tự động, vui lòng không trả lời email này.</p>
                            <p style="margin:0; color:#a0aaba; font-size:11px; line-height:1.6;">Tikzy &middot; Nền tảng đặt vé sự kiện</p>
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

Trân trọng,
Đội ngũ Tikzy
    $text$
WHERE code = 'ACCOUNT_CREATED';

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
<body style="margin:0; padding:0; background-color:#f3f6fb; color:#172033; font-family:Arial, Helvetica, sans-serif; -webkit-text-size-adjust:100%; -ms-text-size-adjust:100%;">
    <div style="display:none; max-height:0; overflow:hidden; opacity:0; color:transparent;">Mã OTP đặt lại mật khẩu Tikzy của bạn.</div>
    <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="width:100%; background-color:#f3f6fb;">
        <tr>
            <td align="center" style="padding:32px 16px;">
                <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="width:100%; max-width:620px; border-radius:20px; overflow:hidden;">
                    <tr>
                        <td style="padding:34px 40px 38px; background-color:#101828; border-top:4px solid #ff6b4a;">
                            <p style="margin:0 0 30px; color:#ffffff; font-size:22px; font-weight:800; letter-spacing:3px;">TIKZY <span style="color:#94a3b8; font-size:10px; font-weight:700; letter-spacing:1.5px;">&middot; ACCOUNT SECURITY</span></p>
                            <span style="display:inline-block; padding:7px 11px; border:1px solid #344054; border-radius:999px; color:#ffab98; font-size:10px; font-weight:700; letter-spacing:1.2px;">BẢO MẬT TÀI KHOẢN</span>
                            <h1 style="margin:18px 0 12px; color:#ffffff; font-size:34px; line-height:1.15; font-weight:800; letter-spacing:-0.8px;">Đặt lại<br>mật khẩu</h1>
                            <p style="margin:0; color:#c6d0df; font-size:15px; line-height:1.7;">Xác thực danh tính để tiếp tục sử dụng tài khoản.</p>
                        </td>
                    </tr>
                    <tr>
                        <td style="padding:40px; background-color:#ffffff;">
                            <p style="margin:0 0 18px; color:#172033; font-size:16px; line-height:1.6;">Xin chào <strong>{{fullName}}</strong>,</p>
                            <p style="margin:0 0 28px; color:#526078; font-size:15px; line-height:1.75;">Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản Tikzy của bạn. Nhập mã OTP bên dưới để xác thực.</p>

                            <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="width:100%; margin-bottom:24px; border:1px solid #e4eaf2; background-color:#fbfcfe;">
                                <tr>
                                    <td align="center" style="padding:24px 20px 22px;">
                                        <p style="margin:0 0 12px; color:#8a96a9; font-size:10px; font-weight:700; letter-spacing:1.4px;">MÃ XÁC THỰC CỦA BẠN</p>
                                        <p style="margin:0; color:#101828; font-size:34px; line-height:1.25; font-weight:800; letter-spacing:10px;">{{otp}}</p>
                                        <p style="margin:14px 0 0; color:#69768c; font-size:13px; line-height:1.6;">Có hiệu lực trong <strong style="color:#172033;">{{expiresInMinutes}} phút</strong></p>
                                    </td>
                                </tr>
                            </table>

                            <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="width:100%; margin-bottom:28px; background-color:#fff8e8;">
                                <tr>
                                    <td width="4" style="width:4px; background-color:#f5b942;"></td>
                                    <td style="padding:16px 18px; color:#745b20; font-size:13px; line-height:1.65;">Nếu bạn không thực hiện yêu cầu này, hãy bỏ qua email. Không chia sẻ mã OTP với bất kỳ ai.</td>
                                </tr>
                            </table>

                            <p style="margin:0; padding-top:24px; border-top:1px solid #edf1f6; color:#526078; font-size:14px; line-height:1.7;">Trân trọng,<br><strong style="color:#172033;">Đội ngũ Tikzy</strong></p>
                        </td>
                    </tr>
                    <tr>
                        <td style="padding:22px 40px; background-color:#f8fafc; border-top:1px solid #e9eef5;">
                            <p style="margin:0 0 6px; color:#8490a3; font-size:11px; line-height:1.6;">Đây là email tự động, vui lòng không trả lời email này.</p>
                            <p style="margin:0; color:#a0aaba; font-size:11px; line-height:1.6;">Tikzy &middot; Nền tảng đặt vé sự kiện</p>
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
<body style="margin:0; padding:0; background-color:#f3f6fb; color:#172033; font-family:Arial, Helvetica, sans-serif; -webkit-text-size-adjust:100%; -ms-text-size-adjust:100%;">
    <div style="display:none; max-height:0; overflow:hidden; opacity:0; color:transparent;">Mã OTP mở khóa tài khoản Tikzy của bạn.</div>
    <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="width:100%; background-color:#f3f6fb;">
        <tr>
            <td align="center" style="padding:32px 16px;">
                <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="width:100%; max-width:620px; border-radius:20px; overflow:hidden;">
                    <tr>
                        <td style="padding:34px 40px 38px; background-color:#101828; border-top:4px solid #ff6b4a;">
                            <p style="margin:0 0 30px; color:#ffffff; font-size:22px; font-weight:800; letter-spacing:3px;">TIKZY <span style="color:#94a3b8; font-size:10px; font-weight:700; letter-spacing:1.5px;">&middot; ACCOUNT RECOVERY</span></p>
                            <span style="display:inline-block; padding:7px 11px; border:1px solid #344054; border-radius:999px; color:#ffab98; font-size:10px; font-weight:700; letter-spacing:1.2px;">KHÔI PHỤC TÀI KHOẢN</span>
                            <h1 style="margin:18px 0 12px; color:#ffffff; font-size:34px; line-height:1.15; font-weight:800; letter-spacing:-0.8px;">Mở khóa<br>tài khoản của bạn</h1>
                            <p style="margin:0; color:#c6d0df; font-size:15px; line-height:1.7;">Thêm một bước xác thực để trở lại với những sự kiện yêu thích.</p>
                        </td>
                    </tr>
                    <tr>
                        <td style="padding:40px; background-color:#ffffff;">
                            <p style="margin:0 0 18px; color:#172033; font-size:16px; line-height:1.6;">Xin chào <strong>{{fullName}}</strong>,</p>
                            <p style="margin:0 0 28px; color:#526078; font-size:15px; line-height:1.75;">Tài khoản Tikzy của bạn đang bị khóa vì đăng nhập sai quá số lần cho phép. Sử dụng mã OTP dưới đây để tiếp tục khôi phục tài khoản.</p>

                            <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="width:100%; margin-bottom:24px; border:1px solid #e4eaf2; background-color:#fbfcfe;">
                                <tr>
                                    <td align="center" style="padding:24px 20px 22px;">
                                        <p style="margin:0 0 12px; color:#8a96a9; font-size:10px; font-weight:700; letter-spacing:1.4px;">MÃ XÁC THỰC CỦA BẠN</p>
                                        <p style="margin:0; color:#101828; font-size:34px; line-height:1.25; font-weight:800; letter-spacing:10px;">{{otp}}</p>
                                        <p style="margin:14px 0 0; color:#69768c; font-size:13px; line-height:1.6;">Có hiệu lực trong <strong style="color:#172033;">{{expiresInMinutes}} phút</strong></p>
                                    </td>
                                </tr>
                            </table>

                            <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="width:100%; margin-bottom:28px; background-color:#fff8e8;">
                                <tr>
                                    <td width="4" style="width:4px; background-color:#f5b942;"></td>
                                    <td style="padding:16px 18px; color:#745b20; font-size:13px; line-height:1.65;">Nếu bạn không thực hiện yêu cầu này, hãy bỏ qua email. Không chia sẻ mã OTP với bất kỳ ai.</td>
                                </tr>
                            </table>

                            <p style="margin:0; padding-top:24px; border-top:1px solid #edf1f6; color:#526078; font-size:14px; line-height:1.7;">Trân trọng,<br><strong style="color:#172033;">Đội ngũ Tikzy</strong></p>
                        </td>
                    </tr>
                    <tr>
                        <td style="padding:22px 40px; background-color:#f8fafc; border-top:1px solid #e9eef5;">
                            <p style="margin:0 0 6px; color:#8490a3; font-size:11px; line-height:1.6;">Đây là email tự động, vui lòng không trả lời email này.</p>
                            <p style="margin:0; color:#a0aaba; font-size:11px; line-height:1.6;">Tikzy &middot; Nền tảng đặt vé sự kiện</p>
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
Tài khoản Tikzy của bạn đang bị khóa vì đăng nhập sai quá số lần cho phép.
Mã OTP để tiếp tục khôi phục tài khoản là: {{otp}}
Mã có hiệu lực trong {{expiresInMinutes}} phút. Không chia sẻ mã này với bất kỳ ai.

Nếu bạn không thực hiện yêu cầu này, hãy bỏ qua email.
    $text$
WHERE code = 'ACCOUNT_UNLOCK_OTP';
