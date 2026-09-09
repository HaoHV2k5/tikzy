#!/bin/sh
set -e

echo "[INFO] Khởi động Tikzy Daily Reporter Daemon (Múi giờ: ${TZ:-UTC})..."

# Cài đặt crontab
crontab /etc/crontabs/reporter

# Tùy chọn chạy thử ngay khi khởi động nếu được yêu cầu (để kiểm tra kết nối)
if [ "$RUN_ON_STARTUP" = "true" ]; then
  echo "[INFO] RUN_ON_STARTUP=true -> Đang chạy kiểm thử báo cáo ngay lập tức..."
  /scripts/daily_report.sh || echo "[WARN] Lỗi khi chạy báo cáo lần đầu, sẽ tiếp tục theo lịch cron."
fi

echo "[INFO] Bắt đầu cron daemon (chạy định kỳ 00:00 hằng ngày)..."
exec crond -f -l 2
