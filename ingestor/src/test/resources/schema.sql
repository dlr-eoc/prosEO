create table if not exists users(
    username varchar(255) not null primary key,
    password varchar(255) not null,
    enabled boolean not null
);

create table if not exists authorities (
    username varchar(255) not null,
    authority varchar(255) not null,
    constraint fk_authorities_users foreign key(username) references users(username)
);

create unique index if not exists ix_auth_username on authorities (username,authority);

create  table if not exists group_authorities (group_id bigint not null, authority varchar(255) not null, primary key (group_id, authority));
create  table if not exists group_members (id bigint not null, group_id bigint not null, username varchar(255) not null, primary key (id));
create  table if not exists groups (id bigint not null, group_name varchar(255) not null, primary key (id));
create  table if not exists groups_group_members (groups_id bigint not null, group_members_id bigint not null, primary key (groups_id, group_members_id));
create  table if not exists users_group_memberships (users_username varchar(255) not null, group_memberships_id bigint not null, primary key (users_username, group_memberships_id));

alter table groups drop constraint if exists UK_7o859iyhxd19rv4hywgdvu2v4;
alter table groups add constraint if not exists UK_7o859iyhxd19rv4hywgdvu2v4 unique (group_name);
alter table groups_group_members drop constraint if exists UK_132lanwqs6liav9syek4s96xv;
alter table groups_group_members add constraint if not exists UK_132lanwqs6liav9syek4s96xv unique (group_members_id);

alter table users_group_memberships drop constraint if exists UK_e2ijwadyxqhcr2aldhs624px;
alter table users_group_memberships add constraint if not exists UK_e2ijwadyxqhcr2aldhs624px unique (group_memberships_id);

alter table authorities add constraint if not exists fk_authorities_users foreign key (username) references users;

alter table group_authorities add constraint if not exists fk_group_authorities_group foreign key (group_id) references groups;
alter table group_members add constraint if not exists fk_group_members_group foreign key (group_id) references groups;
alter table group_members add constraint if not exists fk_group_members_user foreign key (username) references users;
alter table groups_group_members add constraint if not exists FKawl37vgnmf8ny5a9txq0q0mtq foreign key (group_members_id) references group_members;
alter table groups_group_members add constraint if not exists FKfjhm6ctnf3akprkg5ic279dyi foreign key (groups_id) references groups;

alter table users_group_memberships add constraint if not exists FKtodlfclgikl9ionfovl0t7wp0 foreign key (group_memberships_id) references group_members;
alter table users_group_memberships add constraint if not exists FKhbcokg6kjsft20melhs8njcma foreign key (users_username) references users;



