#!/bin/bash -eu

mkdir -p /var/spool/postfix/tachikoma/
chown tachikoma:postdrop /var/spool/postfix/tachikoma/
chown tachikoma:tachikoma /var/spool/postfix/tachikoma/notification_queue || true

while ! nc -z localhost 25
do
  sleep 0.1
done

fixPermissions() {
    # Set correct group on lmtp socket once it exists
    while ! [ -S /var/spool/postfix/tachikoma/incoming_tachikoma ]
    do
      sleep 0.1
    done
    chgrp postfix /var/spool/postfix/tachikoma/incoming_tachikoma
}
rm -f /var/spool/postfix/tachikoma/incoming_tachikoma
fixPermissions &
su tachikoma -c '/opt/tachikoma-postfix-utils/bin/tachikoma-postfix-utils $TACHIKOMA_CONFIG'
