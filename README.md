# Tikzy

Nền tảng bán vé sự kiện trực tuyến theo mô hình **Marketplace**, kết nối **Nhà tổ chức sự kiện (BTC)** với **Người mua vé**.

## Tổng quan kỹ thuật

| Hạng mục | Quyết định |
|----------|-----------|
| Kiến trúc | Modular Monolith (1 Spring Boot app) |
| Backend | Java 17+ / Spring Boot 3.x |
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
- **Ticket & Inventory**: nhiều hạng vé (Early Bird, GA, VIP...), giữ chỗ 15 phút bằng Redis `SETNX`, vé độc lập với QR ký số HMAC-SHA256 (offline-safe).
- **Promotion**: voucher theo % hoặc số tiền cố định, giới hạn lượt dùng, voucher đền bù.
- **Payment (Strategy)**: đa cổng thanh toán + Auto-Refund đảo ngược giao dịch, chỉ hoàn `orders.total_amount`.
- **Settlement**: chốt sổ doanh thu sau show, tính phí dịch vụ, xuất biên bản đối soát (PDF/Excel).
- **Check-in**: quét QR (`html5-qrcode`), chống double check-in bằng DB Unique Constraint.
- **Banner**: slider trang chủ, banner danh mục, lên lịch hiển thị + ưu tiên.
- **Auth & Session**: JWT (Access 15-30p) + Refresh Token (lưu DB), Logout thu hồi, Refresh Token Rotation.

## Quản trị rủi ro traffic cao

| Rủi ro | Giải pháp |
|--------|-----------|
| Overselling | 3 lớp: Redis `SETNX` + DB Update có điều kiện + Postgres `CHECK` constraint |
| Coupon abuse | Redis atomic `DECR` kiểm soát quota voucher |
| Cạn kiệt DB connection | PgBouncer pooler (port 6543) + HikariCP `maximum-pool-size: 15` |
| Batch refund nghẽn | Spring Batch / Redis Queue, chunk 30-50 đơn, nghỉ 200ms giữa request |
| Duplicate refund | Idempotency Key (UUID) + ràng buộc trạng thái `REFUNDED` |
| Mất callback thanh toán | Payment Reconciliation Scheduler quét đơn `PENDING` quá 15 phút |

## Cơ sở dữ liệu

Toàn bộ bảng dùng **UUID** làm khóa chính (`gen_random_uuid()`) — tránh lộ thông tin kinh doanh, dễ scale đa vùng, tương thích Hibernate 6+.

Các bảng chính: `roles`, `users`, `refresh_tokens`, `categories`, `events`, `show_times`, `ticket_types`, `tickets`, `promotions`, `orders`, `order_items`, `payments`, `refund_logs`, `event_broadcasts`, `settlements`, `check_ins`, `ad_packages`, `ad_campaigns`, `banners`.

---

> Chi tiết đầy đủ: xem [`analysis_results.md`](./analysis_results.md).
