-- ============================================================
-- V2__create_trainers.sql
-- ============================================================
CREATE TABLE trainers (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    trainer_id      VARCHAR(255) NULL,
    name            VARCHAR(255) NULL,
    designation     VARCHAR(255) NULL,
    role            VARCHAR(50) NULL,
    created_at      DATETIME NULL,
    updated_at      DATETIME NULL,

    CONSTRAINT uq_trainers_trainer_id UNIQUE (trainer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;