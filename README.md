Tachikoma ESP [![Build Status](https://circleci.com/gh/SourceForgery/tachikoma/tree/master.svg?style=svg)](https://circleci.com/gh/SourceForgery/tachikoma/tree/master)
=============

This will be an Email Service Provider Software suitable for use with large amounts of transactional
emails.

Primary features

* Handle relatively large amount of emails. Small <$100/month PostgreSQL instance is known to handle more than 100k/month, but should probably be able to handle millions.
* Accurately track bounce, delivers, opens & clicks
* Handle unsubscribe properly via replacing links in the email and
  [RFC8058](https://tools.ietf.org/html/rfc8058)
* Block lists for unsubscribed emails (per sender email)
* Zero Downtime upgrades for web server
* No messages lost, not even during upgrade
* Queue outgoing emails until a specific time (schedule emails)
* Template support (mustache templates)
* Handle incoming as well as outgoing emails

Possible later improvements:

* Web API (Currently only one endpoint is available in GraphQL)
* Web UI
* Archiving emails
* Reducing memory/storage footprint of the postfix <-> webserver glue by switching from Kotlin to Golang

**Runtime requirements**

Recommended way of running it is having the webserver running on a cloudserver e.g. in Kubernetes with the
nodes running in the corporate network. There are Dockers images for all part (including the node with postfix installed)
for easier deployment. The PostgreSQL is probably the resource heaviest part, but that along with all other parts
should have linear resource usage when compared to

What it uses:

* Kotlin language (JVM and JS)
* PostgreSQL database
* Postfix email server
* RabbitMQ message broker
* gRPC API

**Concepts / architecture overview**

Tachikoma is focused on having one command & control web server and multiple mail sender/receiver nodes. Each such node is used to send
and/or receive emails from one email domain only.

The nodes all call in to the webserver using gRPC and the nodes do NOT have to be reachable from the webserver using BACKEND accounts.

The BACKEND accounts are used to communicate between the nodes and the webserver. The FRONTEND accounts are for your software to send/receive emails and get tracking data.

The sending/receiving mail server is postfix because of good security track record, easy integration and good performance.

```
                      +--------------------------------------------------+
                      |                  Tachikoma                       |
+-----------------+   | +-----------+   +---------------+    +---------+ |
| client software |-->| | webserver |<--| postfix-utils |<-->| postfix | |
+-----------------+   | +-----------+   +---------------+    +---------+ |
                      +--------------------------------------------------+
```

***Raison d'être***

When sending emails, it's impossible to build and maintain reputation from cloud servers. Having the possibility to have the actual email server
using ip's from the corporate network without significantly adding attack surface will allow for good reputation building.

Sending emails is hard. Hosted ESPs such as Mandrill and Sendinblue exist because of this, and if you send a lot
of emails, they charge a premium for it. Any reputation you gain by sending good transactional emails is
instantly lost if you decide to switch supplier causing emails to be blocked. Sending emails yourself by
integrating with an email server and setting up all the headers is a non-trivial problem even if there are excellent
libraries in at least Java for the parsing part.

Tracking of emails is almost always important and can be a hassle. Having tracking data available in PostgreSQL
makes it very to ingest into business intelligence databases or use directly from the source database.

**Setting up the development environment**

Setting up the different programs necessary to develop (not run)
it in IntelliJ.

* JDK 21

** Running **

To build and start, run

```
./gradlew run
```

That will start the webpack-dev-server at port 8080 (liable to change) and
the webserver (for e.g. gRPC) at port 8070.

To start the docker (with the Postfix email server), first create a file with the format

```properties
# The url including the backend authentication to the tachikoma webserver
TACHIKOMA_URL=http://example.com:xxxxxxxxxxxxxxxxxxxx@172.17.0.1:8070/
# If the mailserver is MX for the domain above (example.com)
MAIL_DOMAIN_MX=false
# The hostname of the smtp server. Used both when sending email and receiving them
TACHIKOMA_HOSTNAME=smtpserver.example.com
```

run

```
docker run --name postfix -it --rm -h tachikoma-postfix \
  -e TACHIKOMA_CONFIG=/etc/tachikoma.config -v $HOME/.tachikoma-postfix.config:/etc/tachikoma.config \
  sourceforgery/tachikoma-postfix:<version>
```

E.g.

```
docker run --name postfix -it --rm -h tachikoma-postfix \
  -e TACHIKOMA_CONFIG=/etc/tachikoma.config -v $HOME/.tachikoma-postfix.config:/etc/tachikoma.config \
  sourceforgery/tachikoma-postfix:0.0.54
```

**Publish and deploying from other docker repository**

To avoid having to merge a lot of manual tagging and editing set
the `snapshotDockerRepo` property and run the task `publishSnapshot`.
This will tag and push the docker image to a different docker repository and
also set this repository in the deployment yaml file
(`build/kubernetes/deployment-webserver.yaml`).
It's also possible to change the version in the docker tag by setting
`snapshotDockerVersion` (e.g. `-PsnapshotDockerVersion=0.0.0-my-special-version`)

For example

```bash
./gradlew publishSnapshot -PsnapshotDockerRepo=gcr.io/my-staging/ -PsnapshotDockerVersion=test1
```

will tag and push the following images:

```
gcr.io/my-staging/tachikoma-webserver:test1
gcr.io/my-staging/tachikoma-postfix:test1
```

and the following will deploy it to your kubernetes environment.

```
kubectl apply -f build/kubernetes/deployment-webserver.yaml
```

**Recommendations**

* Add the function ```gw () { $(git rev-parse --show-toplevel)/gradlew "$@" }``` to avoid having to do ```../../../gradlew```
* Only run ```gradlew build```. ```gradlew clean build``` should not be necessary and slows down development a *lot*.
* Because of my weak Gradle-fu, updated .proto-files does not trigger rebuild of
  the rest of the api-projects. ```gradle clean build``` is necessary,
  *but only in the api projects*.

## Setup ##

Example `/etc/systemd/system/tachikoma-postfix.service`

```
[Unit]
Description=Tachikoma postfix
After=docker.service
Requires=docker.service

[Service]
Type=simple
Environment=name=tachikoma-postfix
Environment=configDir=/opt/example.com
Environment=mailDomain=EXAMPLE.COM
Environment=backendApiKey=XXXXXXXXXXXXXXXXX
Environment=smtpHostname=SMTP.EXAMPLE.COM
Environment=webserverHost=TACHIKOMA-SERVER.EXAMPLE.COM
Environment=image=sourceforgery/tachikoma-postfix:VERSION

ExecStartPre=/usr/bin/docker pull ${image}
ExecStart=/usr/bin/docker run --rm=true -p 25:25 --name=${name} \
  -e MAIL_DOMAIN_MX=false \
  -e TACHIKOMA_HOSTNAME=${smtpHostname}
  -e TACHIKOMA_URL=https://${mailDomain}:${backendApiKey}@${webserverHost} \
  -v ${configDir}/domainkeys:/etc/opendkim/domainkeys \
  -v ${configDir}/postfix:/var/spool/postfix \
  ${image}

ExecStop=-/usr/bin/docker stop ${name}
Restart=always
RestartSec=10s
TimeoutStartSec=5min
[Install]
WantedBy=multi-user.target
```

### Setting up mail domains ###

* Add your email domain e.g example.com to the server configuration `MAIL_DOMAINS=example.com` this is a , separated list for
  multiple domains.
* Setup SPF for your domain using this [tool](https://mxtoolbox.com/SPFRecordGenerator.aspx?domain=example.com)
* Create DKIM certificate for example.com follow these [instructions](http://knowledge.ondmarc.com/en/articles/2141527-generating-1024-bits-dkim-public-and-private-keys-using-openssl-on-a-mac)
* Create directories e.g `mkdir -p /opt/example.com/domainkeys /opt/example.com/postfix`
* Put the private key in `/etc/opendkim/domainkeys` or if you use a docker version make sure that you put the file in the mounted directory
  e.g `/opt/example.com/domainkeys` make sure that you name the file `<DNS_NAME>._domainkey.<DOMAIN>.private` e.g `20180719._domainkey.example.com.private` where DNS_NAME is what you set in the above instructions.

### Postfix settings that differ from default ###

In `/opt/postfix.sh` some default configurations have been altered

By default tachikoma does not reply to the sender with a bounce message, this as the server handles the response instead.
To revert to the default post fix behaviour remove the following line from the file

`postconf -e "bounce_service_name=discard"`

Every _incoming_ email with multiple receivers will be split up into
several identical emails.

```postconf -e "lmtp_destination_recipient_limit=1"```

The default retry behaviour has also been altered for deferred email to retry in 14400s (4 hours) instead of 4000s (just over an hour) and it will keep trying
for three days instead of five. To revert to the default behaviour remove these lines

```
postconf -e "maximal_backoff_time=14400s"
postconf -e "maximal_queue_lifetime=3d"
postconf -e "bounce_queue_lifetime=3d"
```
