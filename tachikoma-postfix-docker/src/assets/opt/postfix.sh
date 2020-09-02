#!/bin/bash -eu

# Make sure rsyslog is started first
while [ ! -e /dev/log ]
do
  sleep 0.1
done

#postconf -F '*/*/chroot = n'


readEnv() {
  local key=$1
  local line=""

  cat ${TACHIKOMA_CONFIG:-${HOME}/.tachikoma.config} | \
    while read line; do
      if [ "${line%%=*}" = "$key" ]; then
        echo "${line#*=}"
        return 0
      fi
    done
  echo $(eval echo \${$key:-})
  return
}

url="$(readEnv TACHIKOMA_URL)"
mailDomainMx="$(readEnv MAIL_DOMAIN_MX)"
hostname="$(readEnv TACHIKOMA_HOSTNAME)"

# Username is maildomain
tmp2="${url#*://}"
MAIL_DOMAIN="${tmp2%%:*}"

echo "@${hostname} whatever" >/etc/postfix/vmailbox

postconf -e myhostname="${hostname}"
postconf -e mydomain="${MAIL_DOMAIN}"

#No local delivery
postconf -e mydestination=

if [ "${mailDomainMx:-false}" = true ]; then
  # Listen for incoming emails to the main domain (i.e. not just the hostname)
  postconf -e virtual_mailbox_domains="$MAIL_DOMAIN,$hostname"
  echo "@$MAIL_DOMAIN whatever" >>/etc/postfix/vmailbox
else
  # Only listen for incoming unsubscribe/bounce emails (i.e. only listen on hostname)
  postconf -e virtual_mailbox_domains="$hostname"
fi

postconf -e "bounce_service_name=discard"
postconf -e "maximal_backoff_time=14400s"
postconf -e "maximal_queue_lifetime=3d"
postconf -e "bounce_queue_lifetime=3d"
postconf -e "lmtp_destination_recipient_limit=1"

postconf -e virtual_transport=lmtp:unix:tachikoma/incoming_tachikoma
postconf -e virtual_mailbox_maps=hash:/etc/postfix/vmailbox

postmap hash:/etc/postfix/vmailbox

# TLS
if [[ -n "$(find /etc/postfix/certs -iname '*.crt')" && -n "$(find /etc/postfix/certs -iname '*.key')" ]]; then
  # shellcheck disable=SC2046
  postconf -e smtpd_tls_cert_file="$(find /etc/postfix/certs -iname '*.crt')"
  postconf -e smtpd_tls_key_file="$(find /etc/postfix/certs -iname '*.key')"
  chmod 400 /etc/postfix/certs/*.*
  postconf -M "submission/inet=submission   inet   n   -   n   -   -   smtpd"
  postconf -P "submission/inet/syslog_name=postfix/submission"
  postconf -P "submission/inet/milter_macro_daemon_name=ORIGINATING"
fi

# OpenDKIM
# shellcheck disable=SC2010
if ls /etc/opendkim/domainkeys/*._domainkey.*.private 2>/dev/null | grep -q domain; then
  postconf -e milter_protocol=2
  postconf -e milter_default_action=accept
  postconf -e smtpd_milters=inet:localhost:8891
  postconf -e non_smtpd_milters=inet:localhost:8891

  # Make sure opendkim is started first
  while ! nc -z localhost 8891
  do
    sleep 0.1
  done
fi

# Set correct group on lmtp socket once it exists
while ! [ -S /var/spool/postfix/tachikoma/incoming_tachikoma ]
do
  sleep 0.1
done
chgrp postfix /var/spool/postfix/tachikoma/incoming_tachikoma

service postfix start

sleep infinity
