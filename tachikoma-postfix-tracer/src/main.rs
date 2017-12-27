extern crate unix_socket;

use unix_socket::{UnixStream, UnixListener};
use std::thread;
use std::collections::HashMap;
use std::io::BufReader;
use std::io::BufRead;
use std::net::Shutdown;

const SOCKET_PATH: &'static str = "/var/spool/postfix/private/tracer_tachikoma";


fn handle_client(stream: UnixStream) {
    let buf_reader = BufReader::new(stream);
    let mut splt = buf_reader.split(0);

    let mut map = HashMap::new();

    let mut key: String = String::new();
    while let Some(Ok(vec)) = splt.next() {
        if vec.is_empty() {
            if !map.is_empty() {
                // Do stuff
                println!("{:?}", map);
                map = HashMap::new();
            }
        } else {
            let str = match String::from_utf8(vec) {
                Ok(str) => str,
                Err(_error) => break
            };
            if key.is_empty() {
                key = str;
            } else {
                map.insert(key, str);
                key = String::new();
            }
        }
    }
    stream.shutdown(Shutdown::Both);
}


fn main() {

    let listener = UnixListener::bind(SOCKET_PATH)
        .expect(&format!("Couldn't open socket {}", SOCKET_PATH));

    for stream in listener.incoming() {
        match stream {
            Ok(stream) => {
                /* connection succeeded */
                thread::spawn(|| handle_client(stream));
            }
            Err(_err) => {
                /* connection failed */
                println!("Failed connection {}", _err.to_string());
                break;
            }
        }
    }
}
