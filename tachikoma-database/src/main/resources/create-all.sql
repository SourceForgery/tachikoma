create table e_account (
  id                            decimal(20) not null,
  mail_domain                   varchar(255) not null,
  version                       bigint not null,
  date_created                  timestamptz not null,
  last_updated                  timestamptz not null,
  constraint pk_e_account primary key (id)
);
create sequence unique_id_seq increment by 1;

create table e_user (
  id                            decimal(20) not null,
  active                        boolean default false not null,
  encrypted_password            varchar(255),
  api_token                     bytea,
  role                          integer not null,
  account_id                    decimal(20),
  recipient_override            varchar(255),
  version                       bigint not null,
  date_created                  timestamptz not null,
  last_updated                  timestamptz not null,
  constraint ck_e_user_role check ( role in (0,1,2)),
  constraint uq_e_user_api_token unique (api_token),
  constraint pk_e_user primary key (id)
);
create sequence unique_id_seq increment by 1;

create table e_blocked_email (
  id                            decimal(20) not null,
  from_email                    varchar(255) not null,
  recipient_email               varchar(255) not null,
  blocked_reason                integer not null,
  account_id                    decimal(20),
  version                       bigint not null,
  date_created                  timestamptz not null,
  last_updated                  timestamptz not null,
  constraint ck_e_blocked_email_blocked_reason check ( blocked_reason in (0,1,2)),
  constraint uq_e_blocked_email_from_email_recipient_email unique (from_email,recipient_email),
  constraint pk_e_blocked_email primary key (id)
);
create sequence unique_id_seq increment by 1;

create table e_email (
  id                            decimal(20) not null,
  body                          TEXT,
  recipient                     varchar(255) not null,
  recipient_name                varchar(255) not null,
  transaction_id                decimal(20),
  message_id                    varchar(255) not null,
  mta_queue_id                  varchar(255),
  version                       bigint not null,
  date_created                  timestamptz not null,
  last_updated                  timestamptz not null,
  constraint uq_e_email_message_id unique (message_id),
  constraint pk_e_email primary key (id)
);
create sequence unique_id_seq increment by 1;

create table e_email_send_transaction (
  id                            decimal(20) not null,
  json_request                  jsonb not null,
  from_email                    varchar(255) not null,
  authentication_id             decimal(20),
  version                       bigint not null,
  date_created                  timestamptz not null,
  last_updated                  timestamptz not null,
  constraint pk_e_email_send_transaction primary key (id)
);
create sequence unique_id_seq increment by 1;

create table e_email_status (
  id                            decimal(20) not null,
  email_status                  varchar(1) not null,
  email_id                      decimal(20),
  mta_status_code               varchar(255),
  date_created                  timestamptz not null,
  constraint ck_e_email_status_email_status check ( email_status in ('5','3','1','2','7','0','4','6')),
  constraint pk_e_email_status primary key (id)
);
create sequence unique_status_event_id_seq increment by 1;

create table e_incoming_email_address (
  id                            decimal(20) not null,
  local_part                    varchar(255),
  mail_domain                   varchar(255) not null,
  account_id                    decimal(20),
  version                       bigint not null,
  date_created                  timestamptz not null,
  last_updated                  timestamptz not null,
  constraint uq_e_incoming_email_address_local_part_mail_domain unique (local_part,mail_domain),
  constraint pk_e_incoming_email_address primary key (id)
);
create sequence unique_id_seq increment by 1;

create table e_incoming_email (
  id                            decimal(20) not null,
  from_email                    varchar(255) not null,
  from_name                     varchar(255) not null,
  receiver_email                varchar(255) not null,
  receiver_name                 varchar(255) not null,
  body                          bytea not null,
  account_dbo_id                decimal(20),
  subject                       varchar(255) not null,
  version                       bigint not null,
  date_created                  timestamptz not null,
  last_updated                  timestamptz not null,
  constraint pk_e_incoming_email primary key (id)
);
create sequence unique_id_seq increment by 1;

alter table e_user add constraint fk_e_user_account_id foreign key (account_id) references e_account (id) on delete restrict on update restrict;
create index ix_e_user_account_id on e_user (account_id);

alter table e_blocked_email add constraint fk_e_blocked_email_account_id foreign key (account_id) references e_account (id) on delete restrict on update restrict;
create index ix_e_blocked_email_account_id on e_blocked_email (account_id);

alter table e_email add constraint fk_e_email_transaction_id foreign key (transaction_id) references e_email_send_transaction (id) on delete restrict on update restrict;
create index ix_e_email_transaction_id on e_email (transaction_id);

alter table e_email_send_transaction add constraint fk_e_email_send_transaction_authentication_id foreign key (authentication_id) references e_user (id) on delete restrict on update restrict;
create index ix_e_email_send_transaction_authentication_id on e_email_send_transaction (authentication_id);

alter table e_email_status add constraint fk_e_email_status_email_id foreign key (email_id) references e_email (id) on delete restrict on update restrict;
create index ix_e_email_status_email_id on e_email_status (email_id);

alter table e_incoming_email_address add constraint fk_e_incoming_email_address_account_id foreign key (account_id) references e_account (id) on delete restrict on update restrict;
create index ix_e_incoming_email_address_account_id on e_incoming_email_address (account_id);

alter table e_incoming_email add constraint fk_e_incoming_email_account_dbo_id foreign key (account_dbo_id) references e_account (id) on delete restrict on update restrict;
create index ix_e_incoming_email_account_dbo_id on e_incoming_email (account_dbo_id);

insert into database_version values (-1);