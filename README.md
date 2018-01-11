Tachikoma ESP [![Build Status](https://travis-ci.org/SourceForgery/tachikoma.svg?branch=master)](https://travis-ci.org/SourceForgery/tachikoma)
=============

This will be an Email Service Provider Software suitable for use with large amounts of transactional
emails.

Primary features
* Handle relatively large amount of emails (sub 100k/month)
* Accurately track bounce, delivers, opens & clicks
* Handle unsubscribe properly via replacing links in the email and
  [RFC8058](https://tools.ietf.org/html/rfc8058)
* Block lists for unsubscribed emails (per sender email)
* Zero Downtime upgrades for web server
* No messages lost, not even during upgrade


Possible later improvements:
* Queue outgoing emails until a specific time
* Web API
* Template support


**Runtime requirements**

Once finished it will primarily be distributed as one or more docker images, but it's nowhere
near there yet. This means that all servers must be installed and configured locally (or inside a
docker with port forward).

What it uses:
* Kotlin language (JVM and JS)
* PostgreSQL database
* Postfix email server
* RabbitMQ message broker
* Rust language
* gRPC API

**Setting up the development environment**

Setting up the different programs necessary to develop (not run)
it in IntelliJ.

* [Rustup](https://www.rustup.rs/) is necessary for IntelliJ and an
  easy way to get rust build env up. Run as user, *not* root
* JDK 8 (JRE is untested)
* IntelliJ plugin for Rust
* IntelliJ TOML plugin
* ssl headers (libssl-dev on Ubuntu)

** Running **

To build and start, run
```
./gradlew run
```

That will start the webpack-dev-server at port 8080 (liable to change) and
the webserver (for e.g. gRPC) at port 8070.


To start the docker (with the Postfix email server), run
```
docker run -it -e MAIL_DOMAIN=<maildomain> -e TACHIKOMA_URL=<webserver url> tachikoma/tachikoma-postfix:<version>
```

E.g.
```
docker run -it -e MAIL_DOMAIN=example.com -e "TACHIKOMA_URL=http://password@172.17.0.1:8070/" tachikoma/tachikoma-postfix:0.0.1-SNAPSHOT
```

**Recommendations**
* Add the function ```gw () { $(git rev-parse --show-toplevel)/gradlew "$@" }``` to avoid having to do ```../../../gradlew```
* Only run ```gradlew build```, ```gradlew clean build``` should not be necessary and slows down development a *lot*.
* Because of my weak Gradle-fu, updated .proto-files does not trigger rebuild of
  the rest of the api-projects. ```gradle clean build``` is necessary,
  *but only in the api projects*


**Getting around some quirks**
1. Build with ```./gradlew build``` in the root (should build cleanly).
2. (May not be necessary) Manually mark all these as Generated sources in the ```Mark Directory as``` context menu.
  * ```tachikoma-backend-api-proto/tachikoma-backend-api-jvm/build/generated/source/proto/main/java```
  * ```tachikoma-backend-api-proto/tachikoma-backend-api-jvm/build/generated/source/proto/main/grpc```
  * ```tachikoma-frontend-api-proto/tachikoma-frontend-api-jvm/build/generated/source/proto/main/java```
  * ```tachikoma-frontend-api-proto/tachikoma-frontend-api-jvm/build/generated/source/proto/main/grpc```
3. Open ```View -> Tool Windows -> Cargo``` and add tachikoma-postfix-binaries as a cargo project
4. When IntelliJ flakes out and complains about trying to use 1.8 stuff on 1.6, go ```Open Module Settings```,
  go ```Facets``` and add Kotlin Facet to _all_ modules (and their partial modules, e.g. main and test) you're having
  problems with. Problem will persist until you catch 'em all.
5. `gw clean build` will only work on new builds OR when the `.gradle` in the tachikoma directory has been removed.
