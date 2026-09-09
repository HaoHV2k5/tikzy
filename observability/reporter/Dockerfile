FROM alpine:3.20

RUN apk add --no-cache curl jq tzdata

ENV TZ=Asia/Ho_Chi_Minh
RUN cp /usr/share/zoneinfo/${TZ} /etc/localtime && echo "${TZ}" > /etc/timezone

WORKDIR /scripts

COPY crontab /etc/crontabs/reporter
COPY daily_report.sh /scripts/daily_report.sh
COPY entrypoint.sh /entrypoint.sh

RUN chmod +x /scripts/daily_report.sh /entrypoint.sh

ENTRYPOINT ["/entrypoint.sh"]
