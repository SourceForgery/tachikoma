#!/bin/bash -eu

if [ ! -f /etc/tachikoma/rsyslog/external.conf ]
then
  rm -f /etc/rsyslog.d/external.conf
fi
/usr/sbin/rsyslogd

# Make sure rsyslog is started first
while [ ! -e /dev/log ]
do
  sleep 0.1
done

configDir=/etc/tachikoma/config

if [ -r $configDir/cloudsql_credentials.json ]
then
  if ! [ -r $configDir/cloudsql_instances ]
  then
    logger -s "No $configDir/cloudsql_instances"
    exit 1
  fi

  while /usr/bin/cloud_sql_proxy \
   -structured_logs \
   -log_debug_stdout=true \
   -credential_file=$configDir/cloudsql_credentials.json \
   -instances="$(cat $configDir/cloudsql_instances)" || true
  do
    sleep 10
  done &
fi

/opt/tachikoma-webserver/bin/tachikoma-webserver