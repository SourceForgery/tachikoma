extern crate protoc_rust_grpc;
extern crate glob;
extern crate tempdir;

use std::path::PathBuf;
use std::path::Path;
use std::fs;
use std::fs::OpenOptions;
use std::io::BufWriter;
use std::io::Write;

const INPUT_DIR: &'static str = "build/grpc-api/";
const OUTPUT_DIR: &'static str = "src/generated_grpc/";

const PROTOC_BIN: &'static str = "/.gradle/caches/modules-2/files-2.1/com.google.protobuf/protoc/3.0.0/**/*.exe";


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

    fs::remove_dir_all(OUTPUT_DIR).is_err();
    fs::create_dir_all(OUTPUT_DIR).expect(format!("Could not create directory {}", OUTPUT_DIR).as_ref());

    let _gradle_protoc_path = add_gradle_protoc_to_path();

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

    let glob_pattern = String::from(OUTPUT_DIR) + "/*.rs";
    for entry in glob::glob(glob_pattern.as_ref()).expect(format!("{} is not found.", INPUT_DIR).as_ref()) {
        if let Ok(path) = entry {
            let pth: PathBuf = path;
            let file_stem = pth.file_stem().expect("No good filename!?").to_str().expect("No good filename2!?");
            if file_stem != "mod" {
                writer.write(format!("pub mod {};\n", file_stem).as_bytes()).expect("Could not write to file");
            }
        }
    }


    fn add_gradle_protoc_to_path() -> Result<tempdir::TempDir, String> {
        let temp_dir = tempdir::TempDir::new("protoc-rust").map_err(|_| String::from("Couldn't create temp directory"))?;
        let protoc_bin = temp_dir.path().join("protoc");

        let home = std::env::var("HOME").map_err(|_| String::from("Expected HOME env variable to be set"))?;
        let gradle_protoc_glob: String = home + PROTOC_BIN;

        let path = std::env::var("PATH").map_err(|_| "Expected PATH env variable to be set")?;
        let temp_dir_string = temp_dir
            .path()
            .to_str()
            .ok_or("")
            .map(String::from)?;

        for entry in glob::glob(gradle_protoc_glob.as_str()).map_err(|_| "Pattern error")? {
            if let Ok(path) = entry {
                println!("{:?}", path);
                std::os::unix::fs::symlink( path, protoc_bin.as_path()).map_err(|_| "Failed to create symlink")?;
            }
        }


        let new_path = temp_dir_string + ":" + path.as_str();
        std::env::set_var("PATH", new_path);
        Ok(temp_dir)
    }
}