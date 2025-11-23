FROM influxdb:2.7

RUN apt-get update && \
    apt-get install -y --no-install-recommends python3-botocore && \
    rm -rf /var/lib/apt/lists/*
