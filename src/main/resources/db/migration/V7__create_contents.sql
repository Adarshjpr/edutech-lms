-- ============================================================
-- V7__create_contents.sql
-- ============================================================
CREATE TABLE contents (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    title               VARCHAR(255) NULL,
    type                VARCHAR(50) NULL,
    link                TEXT NULL,
    announcement_id     BIGINT NULL,
    trainer_id          BIGINT NULL,
    uploaded_at         DATETIME NULL,
    created_at          DATETIME NULL,
    updated_at          DATETIME NULL,

    INDEX idx_contents_announcement_id (announcement_id),
    INDEX idx_contents_trainer_id (trainer_id),

    CONSTRAINT fk_contents_announcement
        FOREIGN KEY (announcement_id) REFERENCES announcements (id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,

    CONSTRAINT fk_contents_trainer
        FOREIGN KEY (trainer_id) REFERENCES trainers (id)
        ON DELETE SET NULL
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;