FROM amazoncorretto:11.0.17

WORKDIR /app

RUN yum -y install unzip

COPY target/universal/analytics-0.1.zip .

RUN unzip analytics-0.1.zip

ENTRYPOINT ["./analytics-0.1/bin/analytics"]
