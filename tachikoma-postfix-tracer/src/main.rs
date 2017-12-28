extern crate unix_socket;
extern crate protobuf;
extern crate grpc;
extern crate tls_api;
extern crate url;

mod generated_grpc;

use generated_grpc::delivery_notifications_grpc::MTADeliveryNotificationsClient;

use grpc::Client;
use std::thread;
use std::collections::HashMap;
use std::io::BufReader;
use std::io::BufRead;
use std::env;
use unix_socket::UnixListener;
use unix_socket::UnixStream;
use url::Url;

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
}

fn setup_grpc() -> MTADeliveryNotificationsClient {
    let args: Vec<String> = env::args().collect();

    let url = Url::parse(&args[0]).expect("First argument must be the url of the server");

    let host = url.host_str().expect("URL needs to have a hostname");
    let port = url.port();
    let conf = grpc::ClientConf::new();

    let client = match url.scheme() {
        "http" => Client::new_plain(host, port.unwrap_or(80), conf),
        // TODO Unable to get this code working fix this
//        "https" => Client::new_tls(host, port.unwrap_or(443), conf),
        _ => panic!("Neither http nor https!")
    }.expect(format!("Could not connect to {}", url).as_ref());
    return MTADeliveryNotificationsClient::with_client(client);
}


fn main() {
    setup_grpc();

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
