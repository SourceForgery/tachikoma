FROM ubuntu:17.10

LABEL maintainer="tachikoma@sourceforgery.com"

RUN apt-get update && \
    apt-get -y upgrade && \
    apt-get -y --no-install-recommends install rabbitmq-server postgresql-contrib-9.6 postgresql-9.6 postgresql-client-9.6 sudo && \
    rm -rf /var/cache/apt/ /var/lib/apt/lists/*

ADD /conf/rabbitmq/* /etc/rabbitmq/
ADD /conf/start.sh /
ADD /conf/patch_pg_conf.sh /etc/postgresql/

RUN /etc/postgresql/patch_pg_conf.sh

RUN rabbitmq-plugins enable --offline rabbitmq_management

# PostgreSQL
EXPOSE 5432

# RabbitMQ
EXPOSE 5672

# RabbitMQ management plugin web interface
EXPOSE 15672

CMD ["/start.sh"]
