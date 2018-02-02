#!/bin/bash -eu

if [[ -z "$(find /etc/opendkim/domainkeys -iname *.private)" ]]; then
  exit 0
fi

adduser postfix opendkim

cat >> /etc/opendkim.conf <<EOF
AutoRestart             Yes
AutoRestartRate         10/1h
UMask                   002
Syslog                  yes
SyslogSuccess           Yes
LogWhy                  Yes
Canonicalization        relaxed/relaxed
KeyTable                refile:/etc/opendkim/KeyTable
SigningTable            refile:/etc/opendkim/SigningTable
Mode                    sv
PidFile                 /var/run/opendkim/opendkim.pid
SignatureAlgorithm      rsa-sha256
UserID                  opendkim:opendkim
Socket                  local:/var/spool/postfix/opendkim/opendkim.sock
EOF

cat >> /etc/opendkim/KeyTable <<EOF
mail._domainkey.$MAIL_DOMAIN $MAIL_DOMAIN:mail:$(find /etc/opendkim/domainkeys -iname *.private)
EOF

cat >> /etc/opendkim/SigningTable <<EOF
*@$MAIL_DOMAIN mail._domainkey.$MAIL_DOMAIN
EOF

mkdir -p /var/spool/postfix/opendkim

chown opendkim:opendkim /var/spool/postfix/opendkim
chmod 0750 /var/spool/postfix/opendkim

chown opendkim:opendkim $(find /etc/opendkim/domainkeys -iname *.private)

chmod 400 $(find /etc/opendkim/domainkeys -iname *.private)

exec /usr/sbin/opendkim -f