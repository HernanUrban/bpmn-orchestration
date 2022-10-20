--liquibase formatted sql

--changeset skynet:create-users
CREATE TABLE users
(
    user_id TEXT PRIMARY KEY,
    external_id TEXT,
    status TEXT NOT NULL,
    cellphone TEXT,
    email TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ
);
--rollback DROP TABLE users;
