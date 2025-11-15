FROM rust:1.80 as builder

USER root
RUN cargo new --bin ingest
WORKDIR /ingest

COPY ./Cargo.lock ./Cargo.toml ./
RUN cargo fetch

COPY ./src ./src

RUN rm -f ./target/release/deps/ingest* && \
    cargo build --release

# runtime
FROM debian:bookworm-slim

RUN apt-get update && \
    apt-get install -y --no-install-recommends libssl3 ca-certificates && \
    rm -rf /var/lib/apt/lists/*

COPY --from=builder /ingest/target/release/ingest .
COPY ./app_config.json ./

CMD ["./ingest"]
