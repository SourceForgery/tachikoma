-- apply changes
create table e_account (
  id                            decimal(20) not null,
  mail_domain                   varchar(255) not null,
  base_url                      varchar(255),
  version                       bigint not null,
  date_created                  timestamp not null,
  last_updated                  timestamp not null,
  constraint uq_e_account_mail_domain unique (mail_domain),
  constraint pk_e_account primary key (id)
);
create sequence unique_id_seq start with 1 increment by 1;

create table e_user (
  id                            decimal(20) not null,
  encrypted_password            varchar(255),
  username                      varchar(255),
  api_token                     varbinary(255),
  role                          integer not null,
  account_id                    decimal(20) not null,
  recipient_override            varchar(255),
  active                        boolean default false not null,
  version                       bigint not null,
  date_created                  timestamp not null,
  last_updated                  timestamp not null,
  constraint ck_e_user_role check ( role in (0,1,2)),
  constraint uq_e_user_username unique (username),
  constraint uq_e_user_api_token unique (api_token),
  constraint pk_e_user primary key (id)
);

create table e_blocked_email (
  id                            decimal(20) not null,
  from_email                    varchar(255) not null,
  recipient_email               varchar(255) not null,
  blocked_reason                integer not null,
  account_id                    decimal(20) not null,
  version                       bigint not null,
  date_created                  timestamp not null,
  last_updated                  timestamp not null,
  constraint ck_e_blocked_email_blocked_reason check ( blocked_reason in (0,1,2)),
  constraint uq_e_blocked_email_from_email_recipient_email unique (from_email,recipient_email),
  constraint pk_e_blocked_email primary key (id)
);

create table e_email (
  id                            decimal(20) not null,
  recipient                     varchar(255) not null,
  recipient_name                varchar(255) not null,
  auto_mail_id                  varchar(255) not null,
  transaction_id                decimal(20) not null,
  message_id                    varchar(255) not null,
  mta_queue_id                  varchar(255),
  meta_data                     clob not null,
  body                          TEXT,
  subject                       TEXT,
  version                       bigint not null,
  date_created                  timestamp not null,
  last_updated                  timestamp not null,
  constraint uq_e_email_auto_mail_id unique (auto_mail_id),
  constraint uq_e_email_message_id unique (message_id),
  constraint pk_e_email primary key (id)
);

create table e_email_send_transaction (
  id                            decimal(20) not null,
  json_request                  clob not null,
  from_email                    varchar(255) not null,
  authentication_id             decimal(20) not null,
  bcc                           varchar array not null,
  meta_data                     clob not null,
  tags                          varchar array not null,
  version                       bigint not null,
  date_created                  timestamp not null,
  last_updated                  timestamp not null,
  constraint pk_e_email_send_transaction primary key (id)
);

create table e_email_status (
  id                            decimal(20) not null,
  email_status                  integer not null,
  email_id                      decimal(20) not null,
  meta_data                     clob not null,
  date_created                  timestamp not null,
  constraint ck_e_email_status_email_status check ( email_status in (0,1,2,3,4,5,6,7)),
  constraint pk_e_email_status primary key (id)
);
create sequence unique_status_event_id_seq start with 1 increment by 1;

create table e_incoming_email_address (
  id                            decimal(20) not null,
  local_part                    varchar(255) not null,
  account_id                    decimal(20) not null,
  version                       bigint not null,
  date_created                  timestamp not null,
  last_updated                  timestamp not null,
  constraint uq_e_incoming_email_address_local_part_account_id unique (local_part,account_id),
  constraint pk_e_incoming_email_address primary key (id)
);

create table e_incoming_email (
  id                            decimal(20) not null,
  mail_from                     varchar(255) not null,
  recipient                     varchar(255) not null,
  from_emails                   clob not null,
  reply_to_emails               clob not null,
  to_emails                     clob not null,
  body                          varbinary(255) not null,
  account_id                    decimal(20) not null,
  subject                       varchar(255) not null,
  version                       bigint not null,
  date_created                  timestamp not null,
  last_updated                  timestamp not null,
  constraint pk_e_incoming_email primary key (id)
);

-- foreign keys and indices
create index ix_e_user_account_id on e_user (account_id);
alter table e_user add constraint fk_e_user_account_id foreign key (account_id) references e_account (id) on delete restrict on update restrict;

create index ix_e_blocked_email_account_id on e_blocked_email (account_id);
alter table e_blocked_email add constraint fk_e_blocked_email_account_id foreign key (account_id) references e_account (id) on delete restrict on update restrict;

create index ix_e_email_transaction_id on e_email (transaction_id);
alter table e_email add constraint fk_e_email_transaction_id foreign key (transaction_id) references e_email_send_transaction (id) on delete restrict on update restrict;

create index ix_e_email_send_transaction_authentication_id on e_email_send_transaction (authentication_id);
alter table e_email_send_transaction add constraint fk_e_email_send_transaction_authentication_id foreign key (authentication_id) references e_user (id) on delete restrict on update restrict;

create index ix_e_email_status_email_id on e_email_status (email_id);
alter table e_email_status add constraint fk_e_email_status_email_id foreign key (email_id) references e_email (id) on delete restrict on update restrict;

create index ix_e_incoming_email_address_account_id on e_incoming_email_address (account_id);
alter table e_incoming_email_address add constraint fk_e_incoming_email_address_account_id foreign key (account_id) references e_account (id) on delete restrict on update restrict;

create index ix_e_incoming_email_account_id on e_incoming_email (account_id);
alter table e_incoming_email add constraint fk_e_incoming_email_account_id foreign key (account_id) references e_account (id) on delete restrict on update restrict;

create index ix_e_email_status_email_status on e_email_status (email_status);
