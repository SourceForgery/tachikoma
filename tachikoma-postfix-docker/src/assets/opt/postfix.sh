#!/bin/bash -eu

#postconf -F '*/*/chroot = n'
#postconf -e myhostname=$MAIL_DOMAIN

postconf -e virtual_transport=lmtp:unix:private/incoming_tachikoma
postconf -e virtual_mailbox_domains=$MAIL_DOMAIN
postconf -e virtual_mailbox_maps=hash:/etc/postfix/vmailbox

echo "@$MAIL_DOMAIN whatever" > /etc/postfix/vmailbox
postmap hash:/etc/postfix/vmailbox

# TLS
if [[ -n "$(find /etc/postfix/certs -iname *.crt)" && -n "$(find /etc/postfix/certs -iname *.key)" ]]; then
  postconf -e smtpd_tls_cert_file=$(find /etc/postfix/certs -iname *.crt)
  postconf -e smtpd_tls_key_file=$(find /etc/postfix/certs -iname *.key)
  chmod 400 /etc/postfix/certs/*.*
  postconf -M submission/inet="submission   inet   n   -   n   -   -   smtpd"
  postconf -P "submission/inet/syslog_name=postfix/submission"
  postconf -P "submission/inet/milter_macro_daemon_name=ORIGINATING"
fi

service postfix start

touch /var/log/mail.log

exec tail -f /var/log/mail.log
