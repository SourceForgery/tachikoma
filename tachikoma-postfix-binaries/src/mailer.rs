extern crate futures;
extern crate grpc;
extern crate lettre;
extern crate protobuf;
extern crate tls_api;
extern crate tls_api_rustls;
extern crate unix_socket;
extern crate url;

mod generated_grpc;
mod common;

use generated_grpc::message_queue::EmailMessage;
use generated_grpc::message_queue::IncomingEmailMessage;
use generated_grpc::message_queue::MTAQueuedNotification;
use generated_grpc::message_queue_grpc::MTAEmailQueueClient;
use generated_grpc::message_queue_grpc::MTAEmailQueue;

use common::setup_grpc::setup_grpc;

use lettre::EmailAddress;
use lettre::EmailTransport;
use lettre::SimpleSendableEmail;
use lettre::smtp::ConnectionReuseParameters;
use lettre::smtp::ClientSecurity;
use lettre::smtp::SmtpTransportBuilder;
use std::env;
use std::fs::remove_file;
use std::io::BufReader;
use std::sync::Arc;
use std::sync::mpsc::Sender;
use std::thread;
use std::vec::Vec;
use unix_socket::UnixListener;
use unix_socket::UnixStream;

const LMTP_SOCKET_PATH: &'static str = "/var/spool/postfix/private/incoming_tachikoma";
#[allow(dead_code)]
const SMTP_OUTGOING_PORT: u16 = 465;


fn handle_client(stream: UnixStream, mta_queue_client: &MTAEmailQueueClient) {
    let _buf_reader = BufReader::new(stream);
    // Become LMTP Server to read email.

    let mut incoming_email_message = IncomingEmailMessage::new();
    incoming_email_message.set_emailAddress(String::from("foobar@example.com"));
    incoming_email_message.set_body(b"Long freaking body".to_vec());
    mta_queue_client.incoming_email(grpc::RequestOptions::new(), incoming_email_message);
}


#[allow(dead_code)]
fn send_email(email_message: &EmailMessage, tx: &Sender<MTAQueuedNotification>) {
    let from = EmailAddress::new(email_message.get_from().to_string());
    let body = email_message.get_body().to_string();

    // Open a local connection on port SMTP_OUTGOING_PORT
    let mut mailer = SmtpTransportBuilder::new(("localhost", SMTP_OUTGOING_PORT), ClientSecurity::None).unwrap()
        .connection_reuse(ConnectionReuseParameters::NoReuse)
        .build();

    // Send the emails
    for receiver in email_message.get_emailAddresses() {
        let email = SimpleSendableEmail::new(
            from.clone(),
            vec![EmailAddress::new(receiver.clone())],
            "".to_string(),
            body.clone(),
        );

        let mailer_result = mailer.send(&email);

        let mut notification = MTAQueuedNotification::new();
        notification.set_recipientEmailAddress(receiver.clone());
        notification.set_emailTransactionId(email_message.get_emailTransactionId());

        if let Ok(mailer_response) = mailer_result {
            let _result_lines_with_message_id = mailer_response.message;
            // TODO extract queue id from postfix response
            notification.set_queueId("Here should be something".to_string());
            notification.set_success(true);
            println!("Email sent");
        } else {
            notification.set_success(false);
            println!("Could not send email: {:?}", mailer_result);
        }
        match tx.send(notification) {
            Err(e) => println!("This is bad! Failed to send: {:?}", e),
            Ok(ok) => println!("This managed to send: {:?}", ok)
        }
    }

    println!("Should've sent message {:?}", email_message);
}

fn listen_for_emails(mta_queue_client: Arc<MTAEmailQueueClient>) {
    let (tx, rx) = std::sync::mpsc::channel();
    let ok_iterator = futures::stream::iter_ok(rx.into_iter());
    let grpc_req_stream = grpc::StreamingRequest::new(ok_iterator);
    let email_stream = mta_queue_client.get_emails(grpc::RequestOptions::new(), grpc_req_stream);
    email_stream.map_items(
        move |email_message| {
            send_email(&email_message, &tx)
        }
    );
}

fn main() {
    println!("Started mailer service");

    if let Err(_) = remove_file(LMTP_SOCKET_PATH) {
        println!("Could not delete file {}", LMTP_SOCKET_PATH);
    }

    let args: Vec<String> = env::args().collect();
    let mta_queue_client = Arc::new(MTAEmailQueueClient::with_client(setup_grpc(args)));

    let reference_counted = Arc::clone(&mta_queue_client);
    thread::spawn(move || listen_for_emails(reference_counted));

    let listener = UnixListener::bind(LMTP_SOCKET_PATH)
        .expect(&format!("Couldn't open socket {}", LMTP_SOCKET_PATH));

    for stream in listener.incoming() {
        println!("Mailer LMTP connection received");

        match stream {
            Ok(stream) => {
                /* connection succeeded */
                let reference_counted = Arc::clone(&mta_queue_client);
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
