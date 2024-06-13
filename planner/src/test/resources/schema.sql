create table IF NOT EXISTS users(
    username varchar(50) not null primary key,
    password varchar(250) not null,
    enabled boolean not null
);

create table IF NOT EXISTS authorities (
    username varchar(50) not null,
    authority varchar(50) not null,
    constraint fk_authorities_users foreign key(username) references users(username)
);

create unique index IF NOT EXISTS ix_auth_username on authorities (username,authority);


CREATE OR REPLACE VIEW product_processing_facilities AS
SELECT p.id AS product_id,
      p.enclosing_product_id AS enclosing_product_id,
      pf.processing_facility_id AS processing_facility_id,
      1 AS depth,
      ARRAY[p.id] AS path,
      false AS cycle
FROM product p JOIN product_file pf ON p.id = pf.product_id;