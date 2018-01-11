#!/bin/bash -eu

#postconf -F '*/*/chroot = n'
postconf -e trace_service_name=tracer_tachikoma
postconf -e myhostname=$MAIL_DOMAIN

service postfix start

touch /var/log/mail.log

exec tail -f /var/log/mail.log
