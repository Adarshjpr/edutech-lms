-- ============================================================
-- V1__create_admins.sql
-- ============================================================
CREATE TABLE admins (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    admin_id        VARCHAR(255) NULL,
    name            VARCHAR(255) NULL,
    email           VARCHAR(255) NULL,
    created_at      DATETIME NULL,
    updated_at      DATETIME NULL,

    CONSTRAINT uq_admins_admin_id UNIQUE (admin_id),
    CONSTRAINT uq_admins_email UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;