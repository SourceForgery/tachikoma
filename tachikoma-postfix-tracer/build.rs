extern crate protoc_rust_grpc;

fn main() {


    protoc_rust_grpc::run(protoc_rust_grpc::Args {
        out_dir: "src",
        includes: &["build/api/"],
        input: &["build/api/test.proto"],
        rust_protobuf: true, // also generate protobuf messages, not just services
    }).expect("protoc-rust-grpc");
}