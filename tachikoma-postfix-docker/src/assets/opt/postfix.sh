#!/bin/bash
service postfix start
touch /var/log/mail.log
tail -f /var/log/mail.log
