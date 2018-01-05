extern crate grpc;

use grpc::Client;

use std::vec::Vec;
use tls_api_rustls::TlsConnector;
use url::Url;


pub fn setup_grpc(args: Vec<String>) -> Client {
    let url = Url::parse(&args[1]).expect("First argument must be the url of the server");

    let host = url.host_str().expect("URL needs to have a hostname");
    let port = url.port();
    let conf = grpc::ClientConf::new();

    let client = match url.scheme() {
        "http" => Client::new_plain(host, port.unwrap_or(80), conf),
        "https" => Client::new_tls::<TlsConnector>(host, port.unwrap_or(443), conf),
        _ => panic!("Neither http nor https!")
    }.expect(format!("Could not connect to {}", url).as_ref());
    return client;
}
