#!/bin/bash -eu

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

tmp1="${url##*@}"
# Hostname is the tachikoma MX domain
TACHIKOMA_HOSTNAME="${tmp1%%[/:]*}"

tmp2="${url#*://}"
# Username is maildomain
MAIL_DOMAIN="${tmp2%%:*}"

echo "@${TACHIKOMA_HOSTNAME} whatever" >/etc/postfix/vmailbox

postconf -e myhostname="${TACHIKOMA_HOSTNAME}"

if [ ${mailDomainMx:-false} = true ]; then
  # Listen for incoming emails to the main domain (i.e. not just the TACHIKOMA_HOSTNAME)
  postconf -e virtual_mailbox_domains="$MAIL_DOMAIN,$TACHIKOMA_HOSTNAME"
  echo "@$MAIL_DOMAIN whatever" >>/etc/postfix/vmailbox
else
  # Only listen for incoming unsubscribe/bounce emails (i.e. only listen on TACHIKOMA_HOSTNAME)
  postconf -e virtual_mailbox_domains="$TACHIKOMA_HOSTNAME"
fi

postconf -e virtual_transport=lmtp:unix:tachikoma/incoming_tachikoma
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
postconf -e smtpd_milters=inet:localhost:8891
postconf -e non_smtpd_milters=inet:localhost:8891

service postfix start

touch /var/log/mail.log

exec tail -f /var/log/mail.log
