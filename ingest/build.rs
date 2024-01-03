extern crate prost_build;

use prost_build::Config;
use std::path::PathBuf;

fn main() {
    let mut config = Config::new();

    let protobuf_root = PathBuf::from("..").join("protobuf");

    let proto_files = [
        protobuf_root.join("crypto_quotation.proto"),
        protobuf_root.join("crypto_trade.proto"),
        protobuf_root.join("market_data.proto"),
        protobuf_root.join("money.proto"),
        protobuf_root.join("stock_quotation.proto"),
        protobuf_root.join("stock_trade.proto"),
        protobuf_root.join("trade_unit.proto"),
    ];

    let out_dir = PathBuf::from("src").join("protobuf");

    for file in &proto_files {
        println!("cargo:rerun-if-changed={}", file.to_string_lossy());
    }

    config
        .out_dir(out_dir)
        .compile_protos(&proto_files, &[&protobuf_root])
        .unwrap_or_else(|e| panic!("Failed to compile Rust code from protobuf: {}", e));
}
