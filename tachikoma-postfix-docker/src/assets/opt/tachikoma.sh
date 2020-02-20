#!/bin/bash -eu

mkdir -p /var/spool/postfix/tachikoma/
chown tachikoma:postdrop /var/spool/postfix/tachikoma/

while ! nc -z localhost 25
do
  sleep 0.1
done

su tachikoma -c '/opt/tachikoma-postfix-utils/bin/tachikoma-postfix-utils $TACHIKOMA_CONFIG'