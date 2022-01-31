Tachikoma ESP [![Build Status](https://circleci.com/gh/SourceForgery/tachikoma/tree/master.svg?style=svg)](https://circleci.com/gh/SourceForgery/tachikoma/tree/master)
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
* gRPC API

**Setting up the development environment**

Setting up the different programs necessary to develop (not run)
it in IntelliJ.

* JDK 11

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


**Getting around some quirks**
1. Build with ```./gradlew build``` in the root (should build cleanly).
2. When IntelliJ flakes out and complains about trying to use 1.8 stuff on 1.6, go ```Open Module Settings```,
  go ```Facets``` and add Kotlin Facet to _all_ modules (and their partial modules, e.g. main and test) you're having
  problems with. Problem will persist until you catch 'em all.

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
