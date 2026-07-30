-- ============================================================
-- V6__create_announcements.sql
-- ============================================================
CREATE TABLE announcements (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    message         TEXT NULL,
    batch_id        BIGINT NULL,
    trainer_id      BIGINT NULL,
    admin_id        BIGINT NULL,
    created_at      DATETIME NULL,
    updated_at      DATETIME NULL,

    INDEX idx_announcements_batch_id (batch_id),
    INDEX idx_announcements_trainer_id (trainer_id),
    INDEX idx_announcements_admin_id (admin_id),

    CONSTRAINT fk_announcements_batch
        FOREIGN KEY (batch_id) REFERENCES batches (id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,

    CONSTRAINT fk_announcements_trainer
        FOREIGN KEY (trainer_id) REFERENCES trainers (id)
        ON DELETE SET NULL
        ON UPDATE CASCADE,

    CONSTRAINT fk_announcements_admin
        FOREIGN KEY (admin_id) REFERENCES admins (id)
        ON DELETE SET NULL
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;