# Tachikoma

## Main features:
* Sending and getting results via gRPC
* Zero downtime deploys/system upgrades
* Work well on clouds
* Use proven MTA (Postfix) to send/receive emails

## Requirements:
* RabbitMQ
* Redis
* Postfix server


## Caveats

* Postfix WILL go down (albeit seldom and for short periods). Solutions:
  1. Backup MX - preferred
  2. Accept this as mail servers WILL retry
* Postfix needs persistent storage



## Detailed features:

* gRPC, different for configuration and sending mail
* gRPC stream of events. Call method to delete/accept event. All events to all listeners
* Api-key with access rights
* Force recieving email matching e.g. domain (for test)
* Message-Id events in rabbit mq.
* Configuration in what? Postgresql?
* Synchronous and asynchronous sending
* Stats in redis?
* Email server: Postfix
* Unsubscribe by sending in URL to "template"
* Block spammed emails by sender and receiver for X days
* Incoming mail and ACK via RPC. I.e. when a message is
  received, the client receiving mails have to ack it.
* Send multiple emails for list of recipients
* Zero downtime with nginx and multiple active nodes
