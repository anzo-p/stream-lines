FROM amazoncorretto:17.0.11 as builder

WORKDIR /app

RUN yum -y install unzip

COPY target/universal/currents-0.1.zip .

RUN unzip currents-0.1.zip

FROM amazoncorretto:17-alpine

WORKDIR /app

RUN apk add --no-cache bash libstdc++

COPY --from=builder /app/currents-0.1 /currents-0.1

ENTRYPOINT ["/currents-0.1/bin/currents"]
