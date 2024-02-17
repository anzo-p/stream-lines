FROM influxdb:2.0

RUN apt-get update && \
    apt-get install -y python3-pip && \
    pip3 install botocore && \
    apt-get clean && rm -rf /var/lib/apt/lists/*
