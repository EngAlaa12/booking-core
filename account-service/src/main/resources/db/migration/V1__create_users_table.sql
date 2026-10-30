CREATE TABLE users
(
    id            UUID PRIMARY KEY,
    email         VARCHAR(255)             NOT NULL,
    password_hash VARCHAR(255)             NOT NULL,
    status        VARCHAR(20)              NOT NULL DEFAULT 'ACTIVE',
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_users_email ON users (lower(email));
