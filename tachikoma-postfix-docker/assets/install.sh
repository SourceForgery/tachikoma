#!/bin/bash

#supervisor
cat > /etc/supervisor/supervisord.conf <<EOF
[supervisord]
nodaemon=true
loglevel=debug

[program:postfix]
command=/opt/postfix.sh
EOF

############
#  postfix
############
cat >> /opt/postfix.sh <<EOF
#!/bin/bash
service postfix start
touch /var/log/mail.log
tail -f /var/log/mail.log
EOF
chmod +x /opt/postfix.sh

# Configure postfix
postconf -e myhostname=$MAIL_DOMAIN
postconf -F '*/*/chroot = n'

postconf -e trace_service_name=tracer_tachikoma
