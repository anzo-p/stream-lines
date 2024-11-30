FROM amazoncorretto:17.0.11 as builder

WORKDIR /app

RUN yum -y install unzip

COPY target/universal/ripples-0.1.zip .

RUN unzip ripples-0.1.zip

FROM amazoncorretto:17-alpine

WORKDIR /app

RUN apk add --no-cache bash libstdc++

COPY --from=builder /app/ripples-0.1 /ripples-0.1

ENTRYPOINT ["/ripples-0.1/bin/ripples"]
