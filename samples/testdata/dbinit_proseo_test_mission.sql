-- Initialize prosEO test database
-- 

-- Create user management tables (as long as not created by prosEO data model)
DROP INDEX IF EXISTS ix_auth_username;
DROP TABLE IF EXISTS authorities;
DROP TABLE IF EXISTS users;

CREATE TABLE users
(
    username character varying(50) NOT NULL,
    password character varying(100) NOT NULL,
    enabled boolean NOT NULL,
    CONSTRAINT users_pkey PRIMARY KEY (username)
);

CREATE TABLE authorities
(
    username character varying(50) NOT NULL,
    authority character varying(50) NOT NULL,
    CONSTRAINT fk_authorities_users FOREIGN KEY (username)
        REFERENCES users (username)
);

CREATE UNIQUE INDEX ix_auth_username
    ON authorities (username, authority);

-- Seed for user management
INSERT INTO users VALUES ('test-proseo', '$2a$10$/YdE7Ba2KKcHp1O0G/zfW.7BQ8wHdHuctq4r7de2TLg90Ve8OwnPq', true);
INSERT INTO authorities VALUES ('test-proseo', 'ROLE_USER');
    