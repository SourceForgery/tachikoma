extern crate protoc_rust_grpc;
extern crate glob;

use std::path::PathBuf;
use std::path::Path;
use std::fs;
use std::fs::OpenOptions;
use std::io::BufWriter;
use std::io::Write;

const INPUT_DIR: &'static str = "build/grpc-api/";
const OUTPUT_DIR: &'static str = "src/generated_grpc/";


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

    fs::remove_dir_all(OUTPUT_DIR).expect(format!("Could not remove directory {}", OUTPUT_DIR).as_ref());
    fs::create_dir_all(OUTPUT_DIR).expect(format!("Could not create directory {}", OUTPUT_DIR).as_ref());

    protoc_rust_grpc::run(protoc_rust_grpc::Args {
        out_dir: OUTPUT_DIR,
        includes: &[INPUT_DIR],
        input: v2.as_slice(),
        rust_protobuf: true, // also generate protobuf messages, not just services
    }).expect("protoc-rust-grpc");


    let options = OpenOptions::new().write(true).create(true).to_owned();
    let path_name = String::from(OUTPUT_DIR) + "mod.rs";
    let path = Path::new(&path_name);
    let file = options.open(&path).expect(format!("Couldn't open {} for writing", path_name).as_ref());
    let mut writer = BufWriter::new(&file);
    writer.write(b"extern crate protobuf;\n\n").expect("Couldn't write to lib.rs");

    let glob_pattern = String::from(OUTPUT_DIR) + "/*.rs";
    for entry in glob::glob(glob_pattern.as_ref()).expect(format!("{} is not found.", INPUT_DIR).as_ref()) {
        if let Ok(path) = entry {
            let pth: PathBuf = path;
            let file_stem = pth.file_stem().expect("No good filename!?").to_str().expect("No good filename2!?");
            if file_stem != "mod" {
                writer.write(format!("pub mod {};\n", file_stem).as_bytes());
            }
        }
    }
}