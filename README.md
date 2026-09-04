# Tikzy

Nền tảng bán vé sự kiện trực tuyến theo mô hình **Marketplace**, kết nối **Nhà tổ chức sự kiện (BTC)** với **Người mua vé**.

## Tech Stack

Các công nghệ chính đang được sử dụng trong hệ thống:

### Application

<p align="center">
  <a href="https://www.java.com/"><img src="https://img.shields.io/badge/Java_21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java 21"></a>
  <a href="https://spring.io/projects/spring-boot"><img src="https://img.shields.io/badge/Spring_Boot_3.5-6DB33F?style=for-the-badge&logo=springboot&logoColor=white" alt="Spring Boot 3.5"></a>
  <a href="https://spring.io/projects/spring-security"><img src="https://img.shields.io/badge/Spring_Security-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white" alt="Spring Security"></a>
  <a href="https://maven.apache.org/"><img src="https://img.shields.io/badge/Maven-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white" alt="Apache Maven"></a>
  <a href="https://mapstruct.org/"><img src="https://img.shields.io/badge/MapStruct_1.6.3-4B5563?style=for-the-badge" alt="MapStruct 1.6.3"></a>
</p>

### Frontend

<p align="center">
  <a href="https://react.dev/"><img src="https://img.shields.io/badge/React_18-20232A?style=for-the-badge&logo=react&logoColor=61DAFB" alt="React 18"></a>
  <a href="https://vite.dev/"><img src="https://img.shields.io/badge/Vite-646CFF?style=for-the-badge&logo=vite&logoColor=white" alt="Vite"></a>
  <a href="https://www.typescriptlang.org/"><img src="https://img.shields.io/badge/TypeScript-3178C6?style=for-the-badge&logo=typescript&logoColor=white" alt="TypeScript"></a>
</p>

### Data & Infrastructure

<p align="center">
  <a href="https://www.postgresql.org/"><img src="https://img.shields.io/badge/PostgreSQL-4169E1?style=for-the-badge&logo=postgresql&logoColor=white" alt="PostgreSQL"></a>
  <a href="https://supabase.com/"><img src="https://img.shields.io/badge/Supabase-3FCF8E?style=for-the-badge&logo=supabase&logoColor=0F172A" alt="Supabase"></a>
  <a href="https://redis.io/"><img src="https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white" alt="Redis"></a>
  <a href="https://documentation.red-gate.com/flyway"><img src="https://img.shields.io/badge/Flyway-CC0200?style=for-the-badge&logo=flyway&logoColor=white" alt="Flyway"></a>
  <a href="https://www.docker.com/"><img src="https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white" alt="Docker"></a>
</p>

### Security & Integrations

<p align="center">
  <a href="https://jwt.io/"><img src="https://img.shields.io/badge/JWT-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white" alt="JSON Web Token"></a>
  <a href="https://cloudinary.com/"><img src="https://img.shields.io/badge/Cloudinary-3448C5?style=for-the-badge&logo=cloudinary&logoColor=white" alt="Cloudinary"></a>
  <a href="https://github.com/zxing/zxing"><img src="https://img.shields.io/badge/ZXing-QR_Code-2F80ED?style=for-the-badge" alt="ZXing QR Code"></a>
  <a href="https://www.brevo.com/"><img src="https://img.shields.io/badge/Brevo-0B996E?style=for-the-badge&logo=brevo&logoColor=white" alt="Brevo Transactional Email"></a>
</p>

> Repository hiện tại tập trung vào backend Spring Boot trong thư mục `backend/`; các badge frontend phản ánh kiến trúc SPA được sử dụng cho Tikzy.

## Tổng quan kỹ thuật

| Hạng mục | Quyết định |
|----------|-----------|
| Kiến trúc | Modular Monolith (1 Spring Boot app) |
| Backend | Java 21 / Spring Boot 3.5.5 |
| Frontend | React 18 + Vite + TypeScript (SPA) |
| Database | Supabase (managed PostgreSQL) — PgBouncer port 6543 |
| Cache / Lock | Redis (cache, session, distributed lock chống oversell) |
| Image Storage | Cloudinary |
| Dòng tiền | **Escrow** — 100% tiền vé giữ tại Tikzy, quyết toán cho BTC sau show |
| Payment | **Strategy Pattern** + **Auto-Refund API** (VNPAY, MoMo) |
| Deploy | VPS + Docker |

## Các nhóm người dùng

- **Khách hàng**: tìm sự kiện, mua vé, áp voucher, thanh toán, nhận vé QR, check-in, hoàn vé/hoàn tiền tự động.
- **Nhà tổ chức (BTC)**: tạo sự kiện & hạng vé, cấp voucher, cấu hình chính sách hoàn vé, quét vé check-in, broadcast thư xin lỗi + voucher đền bù.
- **Admin Tikzy**: duyệt sự kiện, giám sát Escrow, hủy show & chạy hoàn tiền hàng loạt, duyệt quyết toán cho BTC.

## Quy trình nghiệp vụ chính

1. **Mua vé & Voucher**: chọn suất/hạng vé → áp voucher (nếu có) → tính `total_amount` thực trả → thanh toán → phát hành vé QR.
2. **Escrow & Quyết toán**: tiền vé giữ tại Tikzy trong suốt thời gian mở bán → sau show, BTC gửi yêu cầu quyết toán → Admin duyệt → chuyển phần tiền thực nhận (trừ phí nền tảng & phí quảng cáo trả sau).
3. **Hủy show & Auto-Refund**: Admin kích hoạt → hệ thống batch hoàn tiền tự động qua Refund API **đúng `total_amount` thực trả**; voucher không quy đổi tiền mặt.
4. **Hoàn vé theo policy của BTC**: kiểm tra deadline → tính tiền hoàn (trừ phí phạt %) → gọi `PaymentStrategy.refund()`.
5. **Bồi thường voucher**: công cụ trên Organizer Dashboard cho BTC gửi thư xin lỗi + voucher đền bù đến khách từng dùng voucher.

## Module nghiệp vụ

- **Event**: CRUD sự kiện, suất diễn, hạng vé, chính sách hoàn vé (`NO_REFUND` / `ALLOW_REFUND`), upload ảnh Cloudinary.
- **Ticket & Inventory**: nhiều hạng vé (Early Bird, GA, VIP...), mỗi order gắn một suất diễn, inventory theo `show_time + ticket_type`, giữ chỗ 15 phút bằng Redis `SETNX`, vé độc lập với QR ký số HMAC-SHA256 được Backend verify online.
- **Promotion**: voucher theo % hoặc số tiền cố định, giới hạn lượt dùng tổng và theo user, ví voucher, lịch sử sử dụng và voucher đền bù.
- **Payment (Strategy)**: đa cổng thanh toán + Auto-Refund đảo ngược giao dịch, callback idempotent, chỉ hoàn `orders.total_amount`.
- **Settlement**: chốt sổ doanh thu sau show, tính phí dịch vụ, xuất biên bản đối soát (PDF/Excel).
- **Check-in**: quét QR (`html5-qrcode`), Backend verify online, chống double check-in bằng DB Unique Constraint.
- **Banner**: slider trang chủ, banner danh mục, lên lịch hiển thị + ưu tiên.
- **Auth & Session**: JWT (Access 15-30p) + Refresh Token (lưu DB), Logout thu hồi, Refresh Token Rotation.

### Chính sách khóa tài khoản

- Admin cấu hình số lần đăng nhập sai tối đa qua `GET/PATCH /api/v1/admin/security-policy` với trường `maxFailedLoginAttempts` (mặc định `5`).
- Mỗi lần nhập sai mật khẩu làm tăng bộ đếm. Khi đạt ngưỡng, tài khoản bị khóa, toàn bộ session bị thu hồi và access token cũ không còn hợp lệ. Đăng nhập thành công sẽ reset bộ đếm.
- Người dùng gửi email qua `POST /api/v1/auth/account-unlock/request`. Hệ thống chỉ gửi OTP đến email của tài khoản đang bị khóa và luôn trả thông báo chung để tránh lộ thông tin tài khoản.
- Người dùng gửi email và OTP qua `POST /api/v1/auth/account-unlock/verify-otp`. Chỉ OTP hợp lệ mới nhận được reset token dùng một lần.
- Người dùng gửi reset token, `newPassword` và `confirmPassword` qua `POST /api/v1/auth/account-unlock/reset-password`. Thành công mới mở khóa tài khoản, reset bộ đếm và thu hồi session cũ.

## Quản trị rủi ro traffic cao

| Rủi ro | Giải pháp |
|--------|-----------|
| Overselling | 3 lớp: Redis `SETNX` + DB Update có điều kiện + Postgres `CHECK` constraint |
| Coupon abuse | Redis atomic `DECR` kiểm soát quota voucher |
| Cạn kiệt DB connection | PgBouncer pooler (port 6543) + HikariCP `maximum-pool-size: 15` |
| Batch refund nghẽn | Spring Batch / Redis Queue, chunk 30-50 đơn, nghỉ 200ms giữa request |
| Duplicate payment/refund | Unique transaction theo provider, conditional state update, Idempotency Key cho refund và query/retry cùng key |
| Mất callback thanh toán | Payment Reconciliation Scheduler quét đơn `PENDING` quá 15 phút |

## Cơ sở dữ liệu

Toàn bộ bảng dùng **UUID** làm khóa chính (`gen_random_uuid()`) — tránh lộ thông tin kinh doanh, dễ scale đa vùng, tương thích Hibernate 6+.

Các bảng chính: `roles`, `users`, `refresh_tokens`, `security_policies`, `account_unlock_requests`, `categories`, `events`, `show_times`, `ticket_types`, `show_time_ticket_inventories`, `tickets`, `promotions`, `user_promotions`, `promotion_usages`, `orders`, `order_items`, `payments`, `refund_logs`, `event_broadcasts`, `settlements`, `check_ins`, `ad_packages`, `ad_campaigns`, `banners`.

---

> Chi tiết đầy đủ: xem [`analysis_results.md`](./analysis_results.md).
