extern crate protoc_rust_grpc;

fn main() {
    protoc_rust_grpc::run(protoc_rust_grpc::Args {
        out_dir: "src",
        includes: &["proto"],
        input: &["proto/aaa.proto", "proto/bbb.proto"],
        rust_protobuf: true, // also generate protobuf messages, not just services
    }).expect("protoc-rust-grpc");
}