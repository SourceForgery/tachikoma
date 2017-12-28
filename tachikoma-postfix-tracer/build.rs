extern crate protoc_rust_grpc;
extern crate glob;

use glob::glob;
use std::path::PathBuf;

const DIR: &'static str = "build/grpc-api/";


fn main() {
    let mut list: Vec<String> = Vec::new();
    let glob_pattern = String::from(DIR) + "/**/*.proto";
    for entry in glob(glob_pattern.as_ref()).expect(format!("{} is not found.", DIR).as_ref()) {
        if let Ok(path) = entry {
            let pth: PathBuf = path;
            let pth2 = pth.to_str().expect("Encoding error in filename?").to_owned();
            list.push(pth2);
        }
    }
    let v2: Vec<&str> = list.iter().map(|s| &**s).collect();

    protoc_rust_grpc::run(protoc_rust_grpc::Args {
        out_dir: "src/generated-grpc",
        includes: &[DIR],
        input: v2.as_slice(),
        rust_protobuf: true, // also generate protobuf messages, not just services
    }).expect("protoc-rust-grpc");
}