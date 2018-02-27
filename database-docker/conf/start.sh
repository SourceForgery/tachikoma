#!/bin/bash -eu

/etc/init.d/rabbitmq-server start

if rabbitmqctl add_vhost tachikoma; then
	rabbitmqctl set_permissions -p tachikoma guest ".*" ".*" ".*"
fi

/etc/init.d/postgresql start
echo "Creating default user (OK if it fails with already exist)"
sudo -u postgres psql -c "CREATE ROLE username PASSWORD 'password' SUPERUSER CREATEDB CREATEROLE;" || true
sudo -u postgres psql -c "CREATE DATABASE tachikoma OWNER username ENCODING 'UTF8'; "

tail -f /var/log/postgresql/*.log /var/log/rabbitmq/*
