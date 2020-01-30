#!/bin/bash -eu

if ! ls /etc/opendkim/domainkeys/*._domainkey.*.private 2>/dev/null | grep -q domain; then
    echo "No domain keys matching pattern /etc/opendkim/domainkeys/*._domainkey.*.private was found"
    echo "Skipping dkim configuration and startup"
    sleep infinity
fi

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
Socket                  local:/opendkim/opendkim.sock
EOF

echo -n >/etc/opendkim/KeyTable
echo -n >/etc/opendkim/SigningTable
for A in /etc/opendkim/domainkeys/*._domainkey.*.private; do
   nsrecord="${A%%.private}";
   nsrecord="${nsrecord##*/}";
   selector="${nsrecord%%._domainkey.*}";
   domain="${nsrecord##*._domainkey.}";
   echo "$nsrecord $domain:$selector:$A" >>/etc/opendkim/KeyTable;
   echo "*@$domain $nsrecord" >>/etc/opendkim/SigningTable;
done


mkdir -p /opendkim
chown opendkim:opendkim /opendkim
chmod 0750 /opendkim

chown opendkim:opendkim $(find /etc/opendkim/domainkeys -iname *.private)

chmod 400 $(find /etc/opendkim/domainkeys -iname *.private)

exec /usr/sbin/opendkim -f