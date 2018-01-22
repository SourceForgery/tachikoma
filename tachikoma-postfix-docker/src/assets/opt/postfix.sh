#!/bin/bash -eu

#postconf -F '*/*/chroot = n'
postconf -e myhostname=$MAIL_DOMAIN
postconf -e mailbox_transport=lmtp:unix:private/incoming_tachikoma
service postfix start

touch /var/log/mail.log

exec tail -f /var/log/mail.log
