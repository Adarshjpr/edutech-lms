-- ============================================================
-- V4__create_batches.sql
-- ============================================================
CREATE TABLE batches (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_id            VARCHAR(255) NULL,
    batch_name          VARCHAR(255) NULL,
    timing              VARCHAR(255) NULL,
    meet_link           TEXT NULL,
    certificate_link    TEXT NULL,
    current_topic       VARCHAR(255) NULL,
    status              VARCHAR(50) NULL,
    trainer_id          BIGINT NULL,
    created_at          DATETIME NULL,
    updated_at          DATETIME NULL,

    CONSTRAINT uq_batches_batch_id UNIQUE (batch_id),

    INDEX idx_batches_trainer_id (trainer_id),

    CONSTRAINT fk_batches_trainer
        FOREIGN KEY (trainer_id) REFERENCES trainers (id)
        ON DELETE SET NULL
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;