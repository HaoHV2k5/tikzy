#!/bin/sh
set -e

# Cấu hình Mimir & Telegram
MIMIR_URL="${MIMIR_URL:-http://mimir:9009}"

if [ -z "$TELEGRAM_BOT_TOKEN" ] || [ -z "$TELEGRAM_CHAT_ID" ]; then
  echo "[ERROR] TELEGRAM_BOT_TOKEN và TELEGRAM_CHAT_ID phải được thiết lập."
  exit 1
fi

query_mimir() {
  local query="$1"
  curl -s -m 10 -G "${MIMIR_URL}/prometheus/api/v1/query" --data-urlencode "query=${query}" 2>/dev/null
}

query_scalar() {
  local query="$1"
  local res
  res=$(query_mimir "$query")
  echo "$res" | jq -r '.data.result[0].value[1] // empty' 2>/dev/null
}

# 1. Đo lường CPU VPS
CPU_VAL=$(query_scalar '100 * (1 - avg(rate(node_cpu_seconds_total{mode="idle"}[5m])))')
if [ -n "$CPU_VAL" ]; then
  CPU_ROUND=$(printf "%.1f" "$CPU_VAL" 2>/dev/null || echo "$CPU_VAL")
  CPU_TEXT="${CPU_ROUND}%"
else
  CPU_TEXT="N/A"
fi

# 2. Đo lường RAM VPS
RAM_PCT=$(query_scalar '100 * (1 - avg(node_memory_MemAvailable_bytes / node_memory_MemTotal_bytes))')
RAM_USED_GB=$(query_scalar '(node_memory_MemTotal_bytes - node_memory_MemAvailable_bytes) / 1073741824')
RAM_TOTAL_GB=$(query_scalar 'node_memory_MemTotal_bytes / 1073741824')

if [ -n "$RAM_PCT" ] && [ -n "$RAM_USED_GB" ] && [ -n "$RAM_TOTAL_GB" ]; then
  RAM_PCT_ROUND=$(printf "%.1f" "$RAM_PCT" 2>/dev/null || echo "$RAM_PCT")
  RAM_USED_ROUND=$(printf "%.1f" "$RAM_USED_GB" 2>/dev/null || echo "$RAM_USED_GB")
  RAM_TOTAL_ROUND=$(printf "%.1f" "$RAM_TOTAL_GB" 2>/dev/null || echo "$RAM_TOTAL_GB")
  RAM_TEXT="${RAM_PCT_ROUND}% (Đã dùng ${RAM_USED_ROUND}GB / ${RAM_TOTAL_ROUND}GB)"
else
  RAM_TEXT="N/A"
fi

# 3. Đo lường Dung lượng Disk
DISK_PCT=$(query_scalar '100 * (1 - (node_filesystem_avail_bytes{mountpoint="/",fstype!~"tmpfs|overlay|squashfs"} / node_filesystem_size_bytes{mountpoint="/",fstype!~"tmpfs|overlay|squashfs"}))')
DISK_AVAIL_GB=$(query_scalar 'node_filesystem_avail_bytes{mountpoint="/",fstype!~"tmpfs|overlay|squashfs"} / 1073741824')

if [ -n "$DISK_PCT" ] && [ -n "$DISK_AVAIL_GB" ]; then
  DISK_PCT_ROUND=$(printf "%.1f" "$DISK_PCT" 2>/dev/null || echo "$DISK_PCT")
  DISK_AVAIL_ROUND=$(printf "%.1f" "$DISK_AVAIL_GB" 2>/dev/null || echo "$DISK_AVAIL_GB")
  DISK_TEXT="${DISK_PCT_ROUND}% (Còn trống: ${DISK_AVAIL_ROUND}GB)"
else
  DISK_TEXT="N/A"
fi

# 4. Kiểm tra trạng thái các Service
check_service_up() {
  local service_name="$1"
  local val
  val=$(query_scalar "up{service=\"$service_name\"}")
  if [ "$val" = "1" ]; then
    echo "🟢 UP"
  elif [ "$val" = "0" ]; then
    echo "🔴 DOWN"
  else
    echo "⚪ N/A"
  fi
}

BACKEND_STATUS=$(check_service_up "tikzy-backend")
ALLOY_STATUS=$(check_service_up "alloy")
GRAFANA_STATUS=$(check_service_up "grafana")
LOKI_STATUS=$(check_service_up "loki")
MIMIR_STATUS=$(check_service_up "mimir")
TEMPO_STATUS=$(check_service_up "tempo")

# Node exporter / VPS
NODE_VAL=$(query_scalar 'up{service="vps"} or up{instance=~".*9100"}')
if [ "$NODE_VAL" = "1" ]; then
  NODE_STATUS="🟢 UP"
elif [ "$NODE_VAL" = "0" ]; then
  NODE_STATUS="🔴 DOWN"
else
  NODE_STATUS="⚪ N/A"
fi

# Redis container
REDIS_COUNT=$(query_scalar 'count(container_last_seen{name=~"/?tikzy-redis"})')
if [ "$REDIS_COUNT" != "" ] && [ "$REDIS_COUNT" != "0" ]; then
  REDIS_STATUS="🟢 UP"
else
  # Dự phòng nếu absent query
  REDIS_ABSENT=$(query_scalar 'absent(container_last_seen{name=~"/?tikzy-redis"})')
  if [ "$REDIS_ABSENT" = "1" ]; then
    REDIS_STATUS="🔴 DOWN"
  else
    REDIS_STATUS="🟢 UP"
  fi
fi

# Tổng kết
HAS_DOWN=0
for st in "$BACKEND_STATUS" "$REDIS_STATUS" "$GRAFANA_STATUS" "$LOKI_STATUS" "$MIMIR_STATUS" "$TEMPO_STATUS" "$ALLOY_STATUS" "$NODE_STATUS"; do
  if [ "$st" = "🔴 DOWN" ]; then
    HAS_DOWN=1
    break
  fi
done

if [ "$HAS_DOWN" -eq 1 ]; then
  OVERALL_SUMMARY="⚠️ <b>CẢNH BÁO:</b> Có dịch vụ đang gặp sự cố, vui lòng kiểm tra ngay!"
else
  OVERALL_SUMMARY="✅ <b>Trạng thái:</b> Toàn bộ hệ thống đang vận hành ổn định."
fi

REPORT_TIME=$(date "+%H:%M:%S - %d/%m/%Y")

# Định dạng nội dung tin nhắn HTML
MESSAGE="📊 <b>[TIKZY] BÁO CÁO SỨC KHỎE HỆ THỐNG</b>
⏰ <b>Thời gian:</b> ${REPORT_TIME} (GMT+7)
🌍 <b>Môi trường:</b> Production

🖥️ <b>TÀI NGUYÊN VPS:</b>
├─ <b>CPU:</b> ${CPU_TEXT}
├─ <b>RAM:</b> ${RAM_TEXT}
└─ <b>Disk:</b> ${DISK_TEXT}

🚀 <b>TRẠNG THÁI DỊCH VỤ:</b>
├─ <b>Backend API:</b> ${BACKEND_STATUS}
├─ <b>Redis Cache:</b> ${REDIS_STATUS}
├─ <b>Node Exporter:</b> ${NODE_STATUS}
├─ <b>Grafana:</b> ${GRAFANA_STATUS}
├─ <b>Mimir (Metrics):</b> ${MIMIR_STATUS}
├─ <b>Loki (Logs):</b> ${LOKI_STATUS}
├─ <b>Tempo (Traces):</b> ${TEMPO_STATUS}
└─ <b>Alloy:</b> ${ALLOY_STATUS}

${OVERALL_SUMMARY}"

echo "[INFO] Gửi báo cáo định kỳ đến Telegram..."
HTTP_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "https://api.telegram.org/bot${TELEGRAM_BOT_TOKEN}/sendMessage" \
  -d "chat_id=${TELEGRAM_CHAT_ID}" \
  -d "text=${MESSAGE}" \
  -d "parse_mode=HTML")

HTTP_STATUS=$(echo "$HTTP_RESPONSE" | tail -n1)
BODY=$(echo "$HTTP_RESPONSE" | sed '$d')

if [ "$HTTP_STATUS" -eq 200 ]; then
  echo "[INFO] Đã gửi báo cáo thành công!"
else
  echo "[ERROR] Gửi tin nhắn thất bại (HTTP $HTTP_STATUS): $BODY"
  exit 1
fi
