#!/bin/bash -eu

touch /var/log/mail.log
chown syslog /var/log/mail.log

/usr/sbin/rsyslogd -n