extern crate protoc_rust_grpc;
extern crate glob;

use std::path::PathBuf;
use std::fs;

const INPUT_DIR: &'static str = "build/grpc-api/";
const OUTPUT_DIR: &'static str = "src/generated-grpc/";


fn main() {
    let mut list: Vec<String> = Vec::new();
    let glob_pattern = String::from(INPUT_DIR) + "/**/*.proto";
    for entry in glob::glob(glob_pattern.as_ref()).expect(format!("{} is not found.", INPUT_DIR).as_ref()) {
        if let Ok(path) = entry {
            let pth: PathBuf = path;
            let pth2 = pth.to_str().expect("Encoding error in filename?").to_owned();
            list.push(pth2);
        }
    }
    let v2: Vec<&str> = list.iter().map(|s| &**s).collect();

    fs::create_dir_all(OUTPUT_DIR).expect(format!("Could not create directory {}", OUTPUT_DIR).as_ref());

    protoc_rust_grpc::run(protoc_rust_grpc::Args {
        out_dir: OUTPUT_DIR,
        includes: &[INPUT_DIR],
        input: v2.as_slice(),
        rust_protobuf: true, // also generate protobuf messages, not just services
    }).expect("protoc-rust-grpc");
}