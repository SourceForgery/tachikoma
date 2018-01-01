extern crate grpc;
extern crate protobuf;
extern crate tls_api;
extern crate tls_api_rustls;
extern crate unix_socket;
extern crate url;

mod generated_grpc;
mod common;

use generated_grpc::delivery_notifications::DeliveredNotification;
use generated_grpc::delivery_notifications_grpc::MTADeliveryNotificationsClient;
use generated_grpc::delivery_notifications_grpc::MTADeliveryNotifications;

use common::setup_grpc::setup_grpc;

use std::collections::HashMap;
use std::env;
use std::io::BufRead;
use std::io::BufReader;
use std::ops::Deref;
use std::sync::Arc;
use std::thread;
use unix_socket::UnixListener;
use unix_socket::UnixStream;

const SOCKET_PATH: &'static str = "/var/spool/postfix/private/tracer_tachikoma";


fn handle_client(stream: UnixStream, mta_notifier: &MTADeliveryNotificationsClient) {
    let buf_reader = BufReader::new(stream);
    let mut splt = buf_reader.split(0);

    let mut message = HashMap::new();

    let mut key: String = String::new();
    while let Some(Ok(vec)) = splt.next() {
        if vec.is_empty() {
            if !message.is_empty() {
                handle_trace_message(message, mta_notifier);
                message = HashMap::new();
            }
        } else {
            let str = match String::from_utf8(vec) {
                Ok(str) => str,
                Err(_error) => break
            };
            if key.is_empty() {
                key = str;
            } else {
                message.insert(key, str);
                key = String::new();
            }
        }
    }
}

fn handle_trace_message(message: HashMap<String, String>, mta_notifier: &MTADeliveryNotificationsClient) {
    println!("{:?}", message);
    // Decipher what kind of message it is, and send it to mta_notifier

    let mut notification = DeliveredNotification::new();
    notification.set_messageId(String::from("foobar"));
    mta_notifier.delivered(grpc::RequestOptions::new(), notification);
}

fn main() {
    let args: Vec<String> = env::args().collect();
    let mta_notifier = Arc::new(MTADeliveryNotificationsClient::with_client(setup_grpc(args)));

    let listener = UnixListener::bind(SOCKET_PATH)
        .expect(&format!("Couldn't open socket {}", SOCKET_PATH));

    for stream in listener.incoming() {
        match stream {
            Ok(stream) => {
                /* connection succeeded */
                let reference_counted = Arc::clone(&mta_notifier);
                thread::spawn(move || handle_client(stream, reference_counted.deref()));
            }
            Err(_err) => {
                /* connection failed */
                println!("Failed connection {}", _err.to_string());
                break;
            }
        }
    }
}
