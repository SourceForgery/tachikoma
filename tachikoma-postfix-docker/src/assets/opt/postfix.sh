#!/bin/bash -eu
postconf -e myhostname=$MAIL_DOMAIN
service postfix start
touch /var/log/mail.log
exec tail -f /var/log/mail.log
