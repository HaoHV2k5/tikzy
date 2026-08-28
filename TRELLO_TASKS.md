

## 1. Nền tảng và cơ sở dữ liệu

- Task: Xác nhận phạm vi MVP và tiêu chí nghiệm thu cho ba nhóm Khách hàng, BTC và Admin | Người thực hiện: Hào Huỳnh | Trạng thái: Task
- Task: Hoàn thiện cấu hình Spring Boot, Flyway, PostgreSQL Supabase, Redis và Cloudinary | Người thực hiện: Hào Huỳnh | Trạng thái: Process
- Task: Hoàn thiện mapping entity/repository theo schema V1 và V2 | Người thực hiện: Hào Huỳnh | Trạng thái: Process
- Task: Sửa sai lệch độ dài kiểu dữ liệu giữa entity và migration để chạy được `ddl-auto=validate` | Người thực hiện: Hào Huỳnh | Trạng thái: Bug
- Task: Kiểm tra migration V1/V2 trên database sạch và database đã có dữ liệu | Người thực hiện: Hào Huỳnh | Trạng thái: Bug
- Task: Bổ sung kiểm tra extension `pgcrypto` và dữ liệu khởi tạo cho database | Người thực hiện: Hào Huỳnh | Trạng thái: Task
- Task: Seed role, category, gói quảng cáo, tài khoản Admin và dữ liệu demo | Người thực hiện: Hào Huỳnh | Trạng thái: Task
- Task: Chuẩn hóa DTO, mapper, validation, pagination và format response lỗi dùng chung | Người thực hiện: Hào Huỳnh | Trạng thái: Process
- Task: Chuẩn hóa application profile và biến môi trường cho local, staging và production | Người thực hiện: Hào Huỳnh | Trạng thái: Task
- Task: Bổ sung kiểm tra bắt buộc cho secret JWT, QR, database và dịch vụ bên ngoài | Người thực hiện: Hào Huỳnh | Trạng thái: Task
- Task: Đồng bộ cơ chế xác thực mật khẩu Redis giữa Docker Compose và Spring Boot | Người thực hiện: Hào Huỳnh | Trạng thái: Bug
- Task: Bổ sung Dockerfile backend và hướng dẫn khởi động môi trường local | Người thực hiện: Hào Huỳnh | Trạng thái: Task

## 2. Bảo mật, Auth và phân quyền

- Task: Tạo API đăng ký tài khoản Khách hàng với validation email, số điện thoại và mật khẩu | Người thực hiện: Hào Huỳnh | Trạng thái: Task
- Task: Tạo API đăng nhập và cấp cặp Access Token cùng Refresh Token | Người thực hiện: Hào Huỳnh | Trạng thái: Task
- Task: Xây dựng JWT service và JWT authentication filter | Người thực hiện: Hào Huỳnh | Trạng thái: Task
- Task: Lưu Refresh Token kèm thiết bị, IP, thời hạn và trạng thái thu hồi | Người thực hiện: Hào Huỳnh | Trạng thái: Task
- Task: Implement Refresh Token Rotation và phát hiện token đã bị tái sử dụng | Người thực hiện: Hào Huỳnh | Trạng thái: Task
- Task: Tạo API đăng xuất một thiết bị và đăng xuất tất cả thiết bị | Người thực hiện: Hào Huỳnh | Trạng thái: Task
- Task: Tạo API xem và cập nhật thông tin tài khoản | Người thực hiện: Hào Huỳnh | Trạng thái: Task
- Task: Phân quyền ROLE_CUSTOMER, ROLE_ORGANIZER và ROLE_ADMIN cho từng endpoint | Người thực hiện: Hào Huỳnh | Trạng thái: Task
- Task: Sửa lỗi SecurityConfig đang cho phép mọi request và CORS wildcard | Người thực hiện: Hào Huỳnh | Trạng thái: Bug
- Task: Loại bỏ secret mặc định không an toàn và giới hạn CORS theo domain frontend | Người thực hiện: Hào Huỳnh | Trạng thái: Bug
- Task: Bổ sung giới hạn đăng nhập sai, khóa tạm thời và xử lý tài khoản vô hiệu hóa | Người thực hiện: Hào Huỳnh | Trạng thái: Task
- Task: Bổ sung quên mật khẩu, đặt lại mật khẩu và xác minh email | Người thực hiện: Hào Huỳnh | Trạng thái: Task

## 3. Danh mục, sự kiện và banner

- Task: Tạo API CRUD danh mục cho Admin và API danh mục đang hoạt động cho Khách hàng | Người thực hiện: Hào Huỳnh | Trạng thái: Task
- Task: Tạo API CRUD sự kiện cho BTC với trạng thái DRAFT | Người thực hiện: Hào Huỳnh | Trạng thái: Task
- Task: Tạo API quản lý nhiều suất diễn của sự kiện | Người thực hiện: Hào Huỳnh | Trạng thái: Task
- Task: Tạo API quản lý hạng vé, giá vé, giới hạn mỗi đơn và trạng thái bán | Người thực hiện: Hào Huỳnh | Trạng thái: Task
- Task: Khởi tạo inventory theo cặp show time và ticket type | Người thực hiện: Hào Huỳnh | Trạng thái: Task
- Task: Tích hợp upload, cập nhật và xóa ảnh sự kiện qua Cloudinary | Người thực hiện: Hào Huỳnh | Trạng thái: Task
- Task: Tạo API danh sách sự kiện công khai có phân trang và lọc theo danh mục | Người thực hiện: Hào Huỳnh | Trạng thái: Task
- Task: Tạo API tìm kiếm sự kiện theo từ khóa, địa điểm, thời gian và mức giá | Người thực hiện: Hào Huỳnh | Trạng thái: Task
- Task: Tạo API chi tiết sự kiện kèm suất diễn, hạng vé và số lượng còn lại | Người thực hiện: Hào Huỳnh | Trạng thái: Task
- Task: Implement luồng duyệt, từ chối, publish, cancel và kết thúc sự kiện cho Admin | Người thực hiện: Hào Huỳnh | Trạng thái: Task
- Task: Kiểm tra quyền BTC chỉ được sửa sự kiện do mình tạo | Người thực hiện: Hào Huỳnh | Trạng thái: Task
- Task: Kiểm tra policy hoàn vé, deadline và phí phạt khi tạo hoặc cập nhật sự kiện | Người thực hiện: Hào Huỳnh | Trạng thái: Task
- Task: Xác định phạm vi ghế ngồi cho MVP và bổ sung mô hình seat map nếu cần | Người thực hiện: Hào Huỳnh | Trạng thái: Task

## 4. Voucher và khuyến mãi

- Task: Tạo API CRUD voucher cho BTC và Admin | Người thực hiện: Hào Huỳnh | Trạng thái: Task
- Task: Xây dựng logic kiểm tra hạn dùng, sự kiện áp dụng, đơn tối thiểu và trạng thái voucher | Người thực hiện: Hào Huỳnh | Trạng thái: Task
- Task: Hỗ trợ voucher giảm theo phần trăm và giảm số tiền cố định kèm mức giảm tối đa | Người thực hiện: Hào Huỳnh | Trạng thái: Task
- Task: Tạo API cấp voucher vào ví riêng của Khách hàng và API xem ví voucher | Người thực hiện: Hào Huỳnh | Trạng thái: Task
- Task: Xử lý quota tổng và quota mỗi user bằng thao tác atomic, không để dùng vượt giới hạn | Người thực hiện: Hào Huỳnh | Trạng thái: Task
- Task: Quản lý trạng thái voucher RESERVED, COMPLETED, RELEASED và USED | Người thực hiện: Hào Huỳnh | Trạng thái: Task
- Task: Lưu lịch sử áp dụng voucher theo user và order, chống ghi nhận trùng | Người thực hiện: Hào Huỳnh | Trạng thái: Task

## 5. Đặt hàng, tồn kho và giữ chỗ

- Task: Tạo API tạo order gắn với một event, một show time và nhiều hạng vé | Người thực hiện: Hào Huỳnh | Trạng thái: Task
- Task: Tính và lưu snapshot `subtotal`, `discount_amount` và `total_amount` bằng tiền thực trả | Người thực hiện: Hào Huỳnh | Trạng thái: Task
- Task: Tạo cơ chế Redis lock theo show time và ticket type để tránh oversell | Người thực hiện: Hào Huỳnh | Trạng thái: Task
- Task: Thêm database conditional update cho reserved quantity và sold quantity | Người thực hiện: Hào Huỳnh | Trạng thái: Task
- Task: Đặt thời hạn giữ chỗ 15 phút và hiển thị countdown cho Khách hàng | Người thực hiện: Hào Huỳnh | Trạng thái: Task
- Task: Giải phóng tồn kho và voucher khi order hết hạn hoặc thanh toán thất bại | Người thực hiện: Hào Huỳnh | Trạng thái: Task
- Task: Tạo scheduler xử lý order PENDING quá hạn một cách an toàn và lặp lại được | Người thực hiện: Hào Huỳnh | Trạng thái: Task
- Task: Tạo API xem danh sách order, chi tiết order, hủy order và thử lại thanh toán | Người thực hiện: Hào Huỳnh | Trạng thái: Task
- Task: Kiểm tra atomic transaction giữa order, inventory và promotion usage | Người thực hiện: Hào Huỳnh | Trạng thái: Task

## 6. Thanh toán và hoàn tiền

- Task: Định nghĩa PaymentStrategy, PaymentContext và DTO cho payment/refund | Người thực hiện: Hào Huỳnh | Trạng thái: Task
- Task: Tích hợp tạo payment URL và verify chữ ký cho VNPAY | Người thực hiện: Hào Huỳnh | Trạng thái: Task
- Task: Tích hợp tạo payment URL và verify chữ ký cho MoMo | Người thực hiện: Hào Huỳnh | Trạng thái: Task
- Task: Tạo callback/IPN server-to-server và chỉ chấp nhận callback hợp lệ về order, amount và chữ ký | Người thực hiện: Hào Huỳnh | Trạng thái: Task
- Task: Xử lý callback thanh toán lặp lại bằng conditional state transition và idempotency | Người thực hiện: Hào Huỳnh | Trạng thái: Task
- Task: Chỉ phát hành vé và chuyển order sang PAID sau khi gateway xác nhận thành công | Người thực hiện: Hào Huỳnh | Trạng thái: Task
- Task: Tạo API return URL chỉ để hiển thị kết quả, không dùng làm nguồn xác nhận thanh toán | Người thực hiện: Hào Huỳnh | Trạng thái: Task
- Task: Tạo Payment Reconciliation Scheduler kiểm tra payment PENDING quá 15 phút | Người thực hiện: Hào Huỳnh | Trạng thái: Task
- Task: Tạo RefundService hoàn tiền đơn lẻ đúng `orders.total_amount` | Người thực hiện: Hào Huỳnh | Trạng thái: Task
- Task: Lưu refund log với idempotency key, query status và retry cùng một key | Người thực hiện: Hào Huỳnh | Trạng thái: Task
- Task: Tạo API Khách hàng yêu cầu hoàn vé theo policy của BTC | Người thực hiện: Hào Huỳnh | Trạng thái: Task
- Task: Khóa vé, hủy event và đẩy batch refund khi Admin xác nhận hủy show | Người thực hiện: Hào Huỳnh | Trạng thái: Task
- Task: Xử lý batch refund theo chunk, rate limit, retry và chuyển MANUAL_BANK khi cần | Người thực hiện: Hào Huỳnh | Trạng thái: Task
- Task: Cấu hình môi trường sandbox, webhook và credential cho các cổng thanh toán | Người thực hiện: Hào Huỳnh | Trạng thái: Task

## 7. Phát hành vé và check-in

- Task: Phát hành từng ticket riêng sau khi order thanh toán thành công | Người thực hiện: Hào Huỳnh | Trạng thái: Task
- Task: Tạo QR payload ký HMAC-SHA256 gồm ticket, event, show time, hạng vé và thông tin cần thiết | Người thực hiện: Hào Huỳnh | Trạng thái: Task
- Task: Tạo API ví vé, chi tiết vé và tải QR cho Khách hàng | Người thực hiện: Hào Huỳnh | Trạng thái: Task
- Task: Tạo API verify QR online và từ chối QR giả, sai event, sai show time hoặc vé đã hủy | Người thực hiện: Hào Huỳnh | Trạng thái: Task
- Task: Tạo API check-in atomic, chống double check-in bằng unique constraint và update điều kiện | Người thực hiện: Hào Huỳnh | Trạng thái: Task
- Task: Phân quyền scanner cho BTC/staff và cho phép check-in thủ công khi cần | Người thực hiện: Hào Huỳnh | Trạng thái: Task
- Task: Tạo báo cáo số lượng vé đã bán, đã check-in và số liệu theo thời gian thực | Người thực hiện: Hào Huỳnh | Trạng thái: Task

## 8. Broadcast và voucher bồi thường

- Task: Cấu hình SMTP, email template và dịch vụ gửi email dùng chung | Người thực hiện: Hào Huỳnh | Trạng thái: Task
- Task: Tạo API BTC gửi broadcast đến tất cả người mua vé của sự kiện | Người thực hiện: Hào Huỳnh | Trạng thái: Task
- Task: Tạo bộ lọc người đã sử dụng voucher cho broadcast bồi thường | Người thực hiện: Hào Huỳnh | Trạng thái: Task
- Task: Tạo voucher compensation và cấp vào ví user, chống cấp trùng bằng unique key | Người thực hiện: Hào Huỳnh | Trạng thái: Task
- Task: Gửi email bất đồng bộ theo queue/outbox, có retry và ghi nhận trạng thái gửi | Người thực hiện: Hào Huỳnh | Trạng thái: Task
- Task: Tạo lịch sử broadcast, số người nhận và phân quyền BTC theo event | Người thực hiện: Hào Huỳnh | Trạng thái: Task

## 9. Escrow và quyết toán

- Task: Thiết kế sổ cái Escrow và audit log cho dòng tiền thu hộ, refund, phí và payout | Người thực hiện: Hào Huỳnh | Trạng thái: Task
- Task: Tính doanh thu gross, tiền đã refund, phí nền tảng và phí quảng cáo trả sau | Người thực hiện: Hào Huỳnh | Trạng thái: Task
- Task: Tạo API BTC gửi yêu cầu quyết toán kèm thông tin tài khoản ngân hàng | Người thực hiện: Hào Huỳnh | Trạng thái: Task
- Task: Tạo luồng Admin duyệt, từ chối và xác nhận đã chuyển tiền cho BTC | Người thực hiện: Hào Huỳnh | Trạng thái: Task
- Task: Lưu lịch sử payout, mã giao dịch ngân hàng và chống payout trùng | Người thực hiện: Hào Huỳnh | Trạng thái: Task
- Task: Xuất biên bản đối soát theo định dạng PDF và Excel | Người thực hiện: Hào Huỳnh | Trạng thái: Task
- Task: Chặn quyết toán trước khi sự kiện kết thúc và kết thúc thời gian xử lý refund | Người thực hiện: Hào Huỳnh | Trạng thái: Task

## 10. Quảng cáo và banner

- Task: Tạo API Admin quản lý gói quảng cáo và bảng giá | Người thực hiện: Hào Huỳnh | Trạng thái: Task
- Task: Tạo API BTC đăng ký chiến dịch quảng cáo PREPAID hoặc POSTPAID_ESCROW | Người thực hiện: Hào Huỳnh | Trạng thái: Task
- Task: Tạo luồng Admin duyệt chiến dịch và lịch hiển thị banner | Người thực hiện: Hào Huỳnh | Trạng thái: Task
- Task: Tạo API CRUD banner kèm upload Cloudinary, vị trí, ưu tiên và thời gian hiển thị | Người thực hiện: Hào Huỳnh | Trạng thái: Task
- Task: Tạo API public lấy banner đang hoạt động, sắp xếp ưu tiên và cache dữ liệu | Người thực hiện: Hào Huỳnh | Trạng thái: Task
- Task: Trừ phí chiến dịch POSTPAID_ESCROW vào công thức quyết toán | Người thực hiện: Hào Huỳnh | Trạng thái: Task

## 11. Frontend React

- Task: Khởi tạo frontend React 18, Vite, TypeScript và cấu hình biến môi trường | Người thực hiện: Hào Huỳnh | Trạng thái: Task
- Task: Xây dựng design system, layout responsive, loading, empty state và error state | Người thực hiện: Hào Huỳnh | Trạng thái: Task
- Task: Xây dựng màn hình đăng ký, đăng nhập, refresh token và đăng xuất | Người thực hiện: Hào Huỳnh | Trạng thái: Task
- Task: Xây dựng trang chủ với hero banner, danh mục và danh sách sự kiện | Người thực hiện: Hào Huỳnh | Trạng thái: Task
- Task: Xây dựng trang tìm kiếm, lọc và phân trang sự kiện | Người thực hiện: Hào Huỳnh | Trạng thái: Task
- Task: Xây dựng trang chi tiết sự kiện, show time và lựa chọn hạng vé | Người thực hiện: Hào Huỳnh | Trạng thái: Task
- Task: Xây dựng checkout với voucher, thông tin người mua và countdown 15 phút | Người thực hiện: Hào Huỳnh | Trạng thái: Task
- Task: Xây dựng trang kết quả thanh toán và xử lý trạng thái thanh toán | Người thực hiện: Hào Huỳnh | Trạng thái: Task
- Task: Xây dựng trang lịch sử order, ví vé, QR ticket và ví voucher | Người thực hiện: Hào Huỳnh | Trạng thái: Task
- Task: Xây dựng giao diện yêu cầu hoàn vé và theo dõi kết quả refund | Người thực hiện: Hào Huỳnh | Trạng thái: Task
- Task: Xây dựng Organizer Dashboard quản lý sự kiện, show time, hạng vé và inventory | Người thực hiện: Hào Huỳnh | Trạng thái: Task
- Task: Xây dựng Organizer Dashboard quản lý voucher, broadcast, scanner và quyết toán | Người thực hiện: Hào Huỳnh | Trạng thái: Task
- Task: Xây dựng Admin Dashboard duyệt sự kiện, banner, refund, settlement và user role | Người thực hiện: Hào Huỳnh | Trạng thái: Task
- Task: Implement route guard và hiển thị chức năng theo role | Người thực hiện: Hào Huỳnh | Trạng thái: Task
- Task: Bổ sung responsive mobile, accessibility và tương thích các trình duyệt chính | Người thực hiện: Hào Huỳnh | Trạng thái: Task

## 12. Kiểm thử và đảm bảo chất lượng

- Task: Tạo bộ unit test cho pricing, voucher, order state, payment state và refund policy | Người thực hiện: Hào Huỳnh | Trạng thái: Task
- Task: Tạo integration test cho repository, migration, PostgreSQL và Redis | Người thực hiện: Hào Huỳnh | Trạng thái: Task
- Task: Tạo security test cho JWT, Refresh Token, CORS và RBAC | Người thực hiện: Hào Huỳnh | Trạng thái: Task
- Task: Tạo concurrency test cho inventory reservation và voucher quota | Người thực hiện: Hào Huỳnh | Trạng thái: Task
- Task: Tạo test callback thanh toán lặp lại, sai chữ ký, sai amount và refund idempotency | Người thực hiện: Hào Huỳnh | Trạng thái: Task
- Task: Tạo test batch refund, retry, timeout và trường hợp MANUAL_BANK | Người thực hiện: Hào Huỳnh | Trạng thái: Task
- Task: Tạo end-to-end test luồng đăng ký, đặt vé, thanh toán, nhận QR và check-in | Người thực hiện: Hào Huỳnh | Trạng thái: Task
- Task: Tạo frontend test cho form, checkout countdown, route guard và các trạng thái lỗi | Người thực hiện: Hào Huỳnh | Trạng thái: Task
- Task: Kiểm thử tải cho inventory, voucher, payment callback và batch refund | Người thực hiện: Hào Huỳnh | Trạng thái: Task
- Task: Chạy UAT theo checklist ba role và sửa toàn bộ lỗi trước release | Người thực hiện: Hào Huỳnh | Trạng thái: Task

## 13. Vận hành và phát hành

- Task: Bổ sung CI pipeline build, test, lint và kiểm tra migration | Người thực hiện: Hào Huỳnh | Trạng thái: Task
- Task: Bổ sung health check, metrics, structured logging và correlation ID | Người thực hiện: Hào Huỳnh | Trạng thái: Task
- Task: Cấu hình rate limit, backup database, Redis persistence và quy trình restore | Người thực hiện: Hào Huỳnh | Trạng thái: Task
- Task: Cấu hình quản lý secret, HTTPS, domain, CORS production và webhook an toàn | Người thực hiện: Hào Huỳnh | Trạng thái: Task
- Task: Viết tài liệu API Swagger/Postman, hướng dẫn deploy và runbook xử lý sự cố | Người thực hiện: Hào Huỳnh | Trạng thái: Task
- Task: Deploy staging, chạy smoke test và xác nhận rollback | Người thực hiện: Hào Huỳnh | Trạng thái: Task
- Task: Deploy production và thiết lập giám sát lỗi, thanh toán, refund và uptime | Người thực hiện: Hào Huỳnh | Trạng thái: Task
- Task: Chốt checklist release và bàn giao vận hành dự án Tikzy | Người thực hiện: Hào Huỳnh | Trạng thái: Task
