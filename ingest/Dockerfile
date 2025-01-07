FROM rust:1.80 as builder

USER root
RUN cargo new --bin ingest
WORKDIR /ingest

COPY ./Cargo.lock ./Cargo.toml ./

RUN cargo fetch

COPY ./src ./src

RUN rm -f ./target/release/deps/ingest* && \
    cargo build --release

FROM debian:bullseye-20240211-slim

# OpenSSL
RUN apt-get update &&  \
    apt-get install -y libssl1.1 ca-certificates &&  \
    rm -rf /var/lib/apt/lists/*

COPY --from=builder /ingest/target/release/ingest .
COPY ./app_config.json ./

CMD ["./ingest"]
