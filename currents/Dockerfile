FROM amazoncorretto:17.0.11 as builder

WORKDIR /app

RUN yum -y install unzip

COPY target/universal/currents-0.1.zip .

RUN UNZIP_DISABLE_ZIPBOMB_DETECTION=true unzip -o currents-0.1.zip

FROM amazoncorretto:17.0.11

COPY --from=builder /app/currents-0.1 /currents-0.1

WORKDIR /currents-0.1

ENTRYPOINT ["java", "-cp", "lib/*:conf", "net.anzop.Currents"]
