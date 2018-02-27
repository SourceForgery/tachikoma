#!/bin/bash -xe

for A in /etc/postgresql/*/*/postgresql.conf /usr/share/postgresql/*/postgresql.conf.sample; do
	tee -a "$A" <<EOF

listen_addresses = '*'
log_min_duration_statement = 500

EOF
done


for A in /etc/postgresql/*/*/pg_hba.conf /usr/share/postgresql/*/pg_hba.conf.sample; do
	tee -a "$A" <<EOF

host    all             all             0.0.0.0/0               md5

EOF
done