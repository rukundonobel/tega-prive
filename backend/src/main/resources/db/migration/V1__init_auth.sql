-- TEGA PRIVÉ V1: authentication foundation (users + roles)
-- Flyway-managed schema. Hibernate ddl-auto=none; Flyway owns the DDL.

CREATE TABLE IF NOT EXISTS users (
    id            UUID                        NOT NULL PRIMARY KEY,
    email         VARCHAR(255)                NOT NULL,
    password_hash VARCHAR(255)                NOT NULL,
    first_name    VARCHAR(255)                NOT NULL,
    last_name     VARCHAR(255)                NOT NULL,
    phone         VARCHAR(255),
    status        VARCHAR(32)                 NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE     NOT NULL DEFAULT now(),
    updated_at    TIMESTAMP WITH TIME ZONE     NOT NULL DEFAULT now(),
    deleted_at    TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT uq_users_phone UNIQUE (phone)
);

CREATE INDEX IF NOT EXISTS idx_users_email ON users (email);

CREATE TABLE IF NOT EXISTS user_roles (
    user_id UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role    VARCHAR(32) NOT NULL,
    CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role)
);
