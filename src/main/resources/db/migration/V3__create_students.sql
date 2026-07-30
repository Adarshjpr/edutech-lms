-- ============================================================
-- V3__create_students.sql
-- ============================================================
CREATE TABLE students (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id      VARCHAR(255) NOT NULL,
    name            VARCHAR(255) NULL,
    email           VARCHAR(255) NOT NULL,
    course          VARCHAR(255) NULL,
    status          VARCHAR(50) NULL,
    created_at      DATETIME NULL,
    updated_at      DATETIME NULL,

    CONSTRAINT uq_students_student_id UNIQUE (student_id),
    CONSTRAINT uq_students_email UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;