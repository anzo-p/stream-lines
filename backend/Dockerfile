FROM rust:1.80 as builder

USER root
RUN cargo new --bin backend
WORKDIR /backend

COPY ./Cargo.lock ./Cargo.toml ./
RUN cargo fetch

COPY ./src ./src

RUN rm -f ./target/release/deps/backend* && \
    cargo build --release

FROM debian:bookworm-slim

RUN apt-get update && \
    apt-get install -y --no-install-recommends libssl3 ca-certificates && \
    rm -rf /var/lib/apt/lists/*

COPY --from=builder /backend/target/release/backend .

EXPOSE 3030

CMD ["./backend"]

