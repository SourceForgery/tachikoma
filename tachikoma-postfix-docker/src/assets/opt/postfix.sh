#!/bin/bash -eu

#postconf -F '*/*/chroot = n'
#postconf -e myhostname=$MAIL_DOMAIN

tmp1="${TACHIKOMA_URL##*@}"
# Hostname is the tachikoma MX domain
TACHIKOMA_HOSTNAME="${tmp1%%/*}"

tmp2="${TACHIKOMA_URL#*://}"
# Username is maildomain
MAIL_DOMAIN="${tmp2%%:*}"

echo "@${TACHIKOMA_HOSTNAME} whatever" >/etc/postfix/vmailbox

if [ ${MAIL_DOMAIN_MX:=false} = true ]; then
  # Listen for incoming emails to the main domain (i.e. not just the TACHIKOMA_HOSTNAME)
  postconf -e virtual_mailbox_domains="$MAIL_DOMAIN,$TACHIKOMA_HOSTNAME"
  echo "@$MAIL_DOMAIN whatever" >>/etc/postfix/vmailbox
else
  # Only listen for incoming unsubscribe/bounce emails (i.e. only listen on TACHIKOMA_HOSTNAME)
  postconf -e virtual_mailbox_domains="$TACHIKOMA_HOSTNAME"
fi

postconf -e virtual_transport=lmtp:unix:private/incoming_tachikoma
postconf -e virtual_mailbox_maps=hash:/etc/postfix/vmailbox

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

# OpenDKIM
postconf -e milter_protocol=2
postconf -e milter_default_action=accept
postconf -e smtpd_milters=unix:/opendkim/opendkim.sock
postconf -e non_smtpd_milters=unix:/opendkim/opendkim.sock

service postfix start

touch /var/log/mail.log

exec tail -f /var/log/mail.log
