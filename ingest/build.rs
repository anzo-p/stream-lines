extern crate prost_build;

fn main() {
    let mut config = prost_build::Config::new();

    let proto_dir = "../protobuf";

    let proto_files = [
        String::from(proto_dir) + "/crypto_quotation.proto",
        String::from(proto_dir) + "/crypto_trade.proto",
        String::from(proto_dir) + "/stock_quotation.proto",
        String::from(proto_dir) + "/stock_trade.proto",
    ];

    let out_dir = "src/protobuf";

    for file in &proto_files {
        println!("cargo:rerun-if-changed={}", file);
    }

    config
        .out_dir(out_dir)
        .compile_protos(&proto_files, &[proto_dir])
        .unwrap_or_else(|e| panic!("Failed to compile Rust code from protobuf: {}", e));
}
