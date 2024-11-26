#!/bin/bash -eu

mkdir -p /var/spool/postfix/tachikoma/
chown tachikoma:postdrop /var/spool/postfix/tachikoma/
chown tachikoma:tachikoma /var/spool/postfix/tachikoma/notification_queue || true

while ! nc -z localhost 25
do
  sleep 0.1
done

su tachikoma -c '/opt/tachikoma-postfix-utils/bin/tachikoma-postfix-utils $TACHIKOMA_CONFIG'
