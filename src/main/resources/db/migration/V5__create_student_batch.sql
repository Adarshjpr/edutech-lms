-- ============================================================
-- V5__create_student_batch.sql
-- ============================================================
CREATE TABLE student_batch (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id      BIGINT NOT NULL,
    batch_id        BIGINT NOT NULL,
    joined_at       DATETIME NULL,
    created_at      DATETIME NULL,
    updated_at      DATETIME NULL,

    INDEX idx_student_batch_student_id (student_id),
    INDEX idx_student_batch_batch_id (batch_id),

    CONSTRAINT fk_student_batch_student
        FOREIGN KEY (student_id) REFERENCES students (id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,

    CONSTRAINT fk_student_batch_batch
        FOREIGN KEY (batch_id) REFERENCES batches (id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;