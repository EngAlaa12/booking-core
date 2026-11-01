-- Creates one database (and matching owner role) per service.
-- Only account_db is used by Stage 1 today; the others are provisioned
-- ahead of time so transaction-service and notification-service can
-- connect to Postgres without any manual setup once they're implemented.

CREATE USER account WITH PASSWORD 'account';
CREATE DATABASE account_db OWNER account;

CREATE USER txn WITH PASSWORD 'txn';
CREATE DATABASE txn_db OWNER txn;

CREATE USER notification WITH PASSWORD 'notification';
CREATE DATABASE notification_db OWNER notification;
