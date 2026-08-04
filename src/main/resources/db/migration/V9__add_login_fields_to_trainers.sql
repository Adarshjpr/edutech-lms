-- ============================================================
-- V9__add_login_fields_to_trainers.sql
-- Trainer login ke liye email aur password (hashed) columns add kiye
-- ============================================================

ALTER TABLE trainers
    ADD COLUMN email VARCHAR(255) NOT NULL AFTER role,
    ADD COLUMN password VARCHAR(255) NOT NULL AFTER email;

ALTER TABLE trainers
    ADD CONSTRAINT uq_trainers_email UNIQUE (email);