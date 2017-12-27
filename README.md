Tachikoma ESP [![Build Status](https://travis-ci.org/SourceForgery/tachikoma.svg?branch=master)](https://travis-ci.org/SourceForgery/tachikoma)
=============

This will be an Email Service Provider Software suitable for use with large amounts of transactional emails.

Primary features
* Handle relatively large amount of emails (sub 100k/month)
* Accurately track bounce, delivers, opens & clicks
* Handle unsubscribe properly via replacing links in the email and [RFC8058](https://tools.ietf.org/html/rfc8058)
* Block lists for unsubscribed emails (per sender email)
* Zero Downtime upgrades for web server
* No messages lost, not even during upgrade


Possible later improvements:
* Queue outgoing emails until a specific time
* Web API 
* Template support


Getting started (on developing it)
==================================

Once finished it will primarily be distributed as one or more docker images, but it's nowhere
near there yet.

What it uses:
* Kotlin language (JVM and JS)
* PostgreSQL database
* Postfix email server
* RabbitMQ message broker
* Rust language
* gRPC API

