FROM amazoncorretto:11.0.17 as builder

WORKDIR /app

RUN yum -y install unzip

COPY target/universal/analytics-0.1.zip .

RUN unzip analytics-0.1.zip

FROM amazoncorretto:11-alpine

WORKDIR /app

RUN apk add --no-cache bash libstdc++

COPY --from=builder /app/analytics-0.1 /analytics-0.1

ENTRYPOINT ["/analytics-0.1/bin/analytics"]
