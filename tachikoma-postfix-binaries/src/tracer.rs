extern crate grpc;
extern crate protobuf;
extern crate tls_api;
extern crate tls_api_rustls;
extern crate unix_socket;
extern crate url;

mod generated_grpc;
mod common;

use generated_grpc::delivery_notifications::DeliveryNotification;
use generated_grpc::delivery_notifications_grpc::MTADeliveryNotificationsClient;
use generated_grpc::delivery_notifications_grpc::MTADeliveryNotifications;

use common::setup_grpc::setup_grpc;

use std::collections::HashMap;
use std::env;
use std::fs::{remove_file};
use std::io::BufRead;
use std::io::BufReader;
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
    // {"queue_id": "458182054", "original_recipient": "foo@example.net", "notify_flags": "0", "dsn_orig_rcpt": "rfc822;foo@example.net", "flags": "1024", "nrequest": "0", "offset": "258", "recipient": "foo@example.net", "status": "4.4.1"}
    // {"diag_type": "diag_text"}
    // {"mta_type": "mta_mname"}
    // {"reason": "connect to example.net[93.184.216.34]:25: Connection timed out", "action": "delayed"}
    // {"original_recipient": "foo@toface.com", "offset": "256", "status": "2.0.0", "flags": "1024", "action": "relayed", "diag_text": "250 2.0.0 OK 1515166737 d71si1945550lfg.282 - gsmtp", "dsn_orig_rcpt": "rfc822;foo@toface.com", "mta_mname": "aspmx.l.google.com", "recipient": "foo@toface.com", "mta_type": "dns", "queue_id": "2061D205A", "reason": "delivery via aspmx.l.google.com[173.194.222.27]:25: 250 2.0.0 OK 1515166737 d71si1945550lfg.282 - gsmtp", "nrequest": "0", "notify_flags": "0", "diag_type": "smtp"}

    if let Some(status) = message.remove("status") {
        if let Some(queue_id) = message.remove("queue_id") {
            if let Some(original_recipient) = message.remove("original_recipient") {
                let mut notification = DeliveryNotification::new();
                notification.set_queueId(queue_id);
                notification.set_status(status);
                notification.set_originalRecipient(original_recipient);

                notification.set_diagnoseText(message.remove("diag_text").unwrap_or_else(|| String::new()));
                notification.set_reason(message.remove("reason").unwrap_or_else(|| String::new()));

                mta_notifier.set_delivery_status(grpc::RequestOptions::new(), notification);
            }
        }
    }
}

fn main() {
    println!("Started tracer service");

    if let Err(_) = remove_file(SOCKET_PATH) {
        println!("Could not delete file {}", SOCKET_PATH);
    }

    let args: Vec<String> = env::args().collect();
    let mta_notifier = Arc::new(MTADeliveryNotificationsClient::with_client(setup_grpc(args)));

    let listener = UnixListener::bind(SOCKET_PATH)
        .expect(&format!("Couldn't open socket {}", SOCKET_PATH));

    for stream in listener.incoming() {
        println!("Tracer connection received");

        match stream {
            Ok(stream) => {
                /* connection succeeded */
                let reference_counted = Arc::clone(&mta_notifier);
                thread::spawn(move || handle_client(stream, &reference_counted));
            }
            Err(_err) => {
                /* connection failed */
                println!("Failed connection {}", _err.to_string());
                break;
            }
        }
    }
}
