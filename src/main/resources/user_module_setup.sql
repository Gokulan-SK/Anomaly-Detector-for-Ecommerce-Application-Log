-- ============================================================
-- anomalydb: User Module Setup SQL
-- Run this ONCE in MySQL against the anomalydb database.
--
-- NOTE: The `username` column on `anomaly_events` is handled
-- automatically by Hibernate (ddl-auto=update). You only need
-- to run this file to create the users table.
-- ============================================================

USE anomalydb;

-- ============================================================
-- USERS TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS users (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    username    VARCHAR(100) NOT NULL,
    password    VARCHAR(255) NOT NULL,
    role        VARCHAR(20)  NOT NULL DEFAULT 'ANALYST',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    CONSTRAINT uq_users_username UNIQUE (username)
);

-- ============================================================
-- SEED: Default admin and analyst accounts
-- Passwords are plain text as per project requirements.
-- Change these before any real deployment.
-- ============================================================
INSERT IGNORE INTO users (username, password, role) VALUES
    ('admin',   'admin123',   'ADMIN'),
    ('analyst1','analyst123', 'ANALYST'),
    ('analyst2','analyst123', 'ANALYST');
