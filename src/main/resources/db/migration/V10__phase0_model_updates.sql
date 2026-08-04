-- ============================================================================
-- V10 : Phase 0 Model Updates
-- ============================================================================
-- Isme 4 kaam ho rahe hain:
--   1. Purani tables me naye columns
--   2. Existing rows ka data backfill (NOT NULL lagane se pehle)
--   3. 2 nayi tables : approval_requests, group_messages
--   4. Indexes + Foreign Keys
-- ============================================================================


-- ============================================================================
-- 1. ADMINS
-- ============================================================================
ALTER TABLE admins
    ADD COLUMN password VARCHAR(255) NULL AFTER email,
    ADD COLUMN phone    VARCHAR(15)  NULL AFTER password,
    ADD COLUMN active   BOOLEAN      NOT NULL DEFAULT TRUE;

-- Purane admins ke paas password nahi tha.
-- Placeholder daal rahe hain -> inhe "Forgot Password" se reset karna padega.
UPDATE admins SET password = 'NEEDS_RESET' WHERE password IS NULL;

ALTER TABLE admins
    MODIFY COLUMN password VARCHAR(255) NOT NULL,
    MODIFY COLUMN admin_id VARCHAR(255) NOT NULL,
    MODIFY COLUMN name     VARCHAR(255) NOT NULL,
    MODIFY COLUMN email    VARCHAR(255) NOT NULL;


-- ============================================================================
-- 2. TRAINERS
-- ============================================================================
ALTER TABLE trainers
    ADD COLUMN username    VARCHAR(255) NULL AFTER name,
    ADD COLUMN phone       VARCHAR(15)  NULL,
    ADD COLUMN first_login BOOLEAN      NOT NULL DEFAULT TRUE,
    ADD COLUMN active      BOOLEAN      NOT NULL DEFAULT TRUE;

-- username unique hona chahiye.
-- trainer_id already unique hai, isliye usi ka lowercase le rahe hain.
-- Example: TR101 -> tr101
UPDATE trainers SET username = LOWER(trainer_id) WHERE username IS NULL;

ALTER TABLE trainers
    MODIFY COLUMN username   VARCHAR(255) NOT NULL,
    MODIFY COLUMN trainer_id VARCHAR(255) NOT NULL,
    MODIFY COLUMN name       VARCHAR(255) NOT NULL,
    MODIFY COLUMN role       VARCHAR(50)  NOT NULL;

ALTER TABLE trainers
    ADD CONSTRAINT uk_trainer_username UNIQUE (username);


-- ============================================================================
-- 3. STUDENTS
-- ============================================================================
-- NOTE: "Course" field ka column already "course" hi bana tha
--       (Spring ki naming strategy ne handle kar liya tha),
--       isliye RENAME ki zarurat nahi hai.

ALTER TABLE students
    ADD COLUMN phone               VARCHAR(15)  NULL,
    ADD COLUMN password            VARCHAR(255) NULL,
    ADD COLUMN approval_status     VARCHAR(20)  NOT NULL DEFAULT 'APPROVED',
    ADD COLUMN added_by_trainer_id BIGINT       NULL;

ALTER TABLE students
    ADD CONSTRAINT fk_student_added_by_trainer
        FOREIGN KEY (added_by_trainer_id) REFERENCES trainers (id);


-- ============================================================================
-- 4. BATCHES
-- ============================================================================
ALTER TABLE batches
    ADD COLUMN topic_updated_at      DATETIME    NULL,
    ADD COLUMN start_date            DATE        NULL,
    ADD COLUMN end_date              DATE        NULL,
    ADD COLUMN approval_status       VARCHAR(20) NOT NULL DEFAULT 'APPROVED',
    ADD COLUMN created_by_trainer_id BIGINT      NULL,
    ADD COLUMN created_by_admin_id   BIGINT      NULL;

ALTER TABLE batches
    MODIFY COLUMN batch_id   VARCHAR(255) NOT NULL,
    MODIFY COLUMN batch_name VARCHAR(255) NOT NULL;

ALTER TABLE batches
    ADD CONSTRAINT fk_batch_created_by_trainer
        FOREIGN KEY (created_by_trainer_id) REFERENCES trainers (id),
    ADD CONSTRAINT fk_batch_created_by_admin
        FOREIGN KEY (created_by_admin_id) REFERENCES admins (id);


-- ============================================================================
-- 5. ANNOUNCEMENTS
-- ============================================================================
ALTER TABLE announcements
    ADD COLUMN title           VARCHAR(255) NULL FIRST,
    ADD COLUMN scope           VARCHAR(20)  NULL,
    ADD COLUMN pinned          BOOLEAN      NOT NULL DEFAULT FALSE,
    ADD COLUMN mail_sent       BOOLEAN      NOT NULL DEFAULT FALSE,
    ADD COLUMN mail_sent_at    DATETIME     NULL,
    ADD COLUMN recipient_count INT          NOT NULL DEFAULT 0,
    ADD COLUMN active          BOOLEAN      NOT NULL DEFAULT TRUE;

-- Purane announcements me title tha hi nahi.
-- Message ke pehle 50 characters se bana rahe hain.
UPDATE announcements
   SET title = CONCAT(LEFT(COALESCE(message, 'Announcement'), 50), '...')
 WHERE title IS NULL;

-- Purane records ka scope guess kar rahe hain:
--   batch juda hai      -> SPECIFIC_BATCH
--   admin ne bheja tha  -> GLOBAL
--   warna               -> ALL_MY_BATCHES
UPDATE announcements
   SET scope = CASE
                 WHEN batch_id IS NOT NULL THEN 'SPECIFIC_BATCH'
                 WHEN admin_id IS NOT NULL THEN 'GLOBAL'
                 ELSE 'ALL_MY_BATCHES'
               END
 WHERE scope IS NULL;

-- Bina message wale record ka koi matlab nahi
UPDATE announcements SET message = 'N/A' WHERE message IS NULL;

ALTER TABLE announcements
    MODIFY COLUMN title   VARCHAR(255) NOT NULL,
    MODIFY COLUMN scope   VARCHAR(20)  NOT NULL,
    MODIFY COLUMN message TEXT         NOT NULL;

CREATE INDEX idx_ann_batch_created ON announcements (batch_id, created_at);


-- ============================================================================
-- 6. CONTENTS
-- ============================================================================
ALTER TABLE contents
    ADD COLUMN description           TEXT         NULL,
    ADD COLUMN cloudinary_public_id  VARCHAR(255) NULL,
    ADD COLUMN file_size             BIGINT       NULL,
    ADD COLUMN batch_id              BIGINT       NULL,
    ADD COLUMN uploaded_by_admin_id  BIGINT       NULL,
    ADD COLUMN active                BOOLEAN      NOT NULL DEFAULT TRUE;

-- batch_id ab NOT NULL hona hai, lekin purani rows me hai nahi.
-- Announcement ke through nikalne ki koshish kar rahe hain.
UPDATE contents c
  JOIN announcements a ON c.announcement_id = a.id
   SET c.batch_id = a.batch_id
 WHERE c.batch_id IS NULL
   AND a.batch_id IS NOT NULL;

-- Jinka batch phir bhi nahi mila, unhe pehle available batch me daal rahe hain.
-- (Orphan content ko delete karne se better hai)
UPDATE contents
   SET batch_id = (SELECT MIN(id) FROM batches)
 WHERE batch_id IS NULL;

-- uploaded_at khali ho to created_at se bhar do
UPDATE contents
   SET uploaded_at = COALESCE(created_at, NOW())
 WHERE uploaded_at IS NULL;

UPDATE contents SET link  = 'N/A' WHERE link  IS NULL;
UPDATE contents SET title = 'Untitled' WHERE title IS NULL;
UPDATE contents SET type  = 'LINK' WHERE type  IS NULL;

ALTER TABLE contents
    MODIFY COLUMN batch_id    BIGINT       NOT NULL,
    MODIFY COLUMN title       VARCHAR(255) NOT NULL,
    MODIFY COLUMN type        VARCHAR(50)  NOT NULL,
    MODIFY COLUMN link        TEXT         NOT NULL,
    MODIFY COLUMN uploaded_at DATETIME     NOT NULL;

ALTER TABLE contents
    ADD CONSTRAINT fk_content_batch
        FOREIGN KEY (batch_id) REFERENCES batches (id),
    ADD CONSTRAINT fk_content_uploaded_by_admin
        FOREIGN KEY (uploaded_by_admin_id) REFERENCES admins (id);

CREATE INDEX idx_content_batch_uploaded ON contents (batch_id, uploaded_at);


-- ============================================================================
-- 7. STUDENT_BATCH
-- ============================================================================
ALTER TABLE student_batch
    ADD COLUMN left_at DATETIME NULL,
    ADD COLUMN active  BOOLEAN  NOT NULL DEFAULT TRUE;

UPDATE student_batch SET joined_at = COALESCE(created_at, NOW()) WHERE joined_at IS NULL;

ALTER TABLE student_batch
    MODIFY COLUMN joined_at DATETIME NOT NULL;

-- Unique constraint lagane se PEHLE duplicates hatana zaroori hai,
-- warna migration fail ho jayegi.
-- Har (student, batch) pair ka sabse purana record rakhenge, baaki delete.
DELETE sb1
  FROM student_batch sb1
  JOIN student_batch sb2
    ON sb1.student_id = sb2.student_id
   AND sb1.batch_id   = sb2.batch_id
   AND sb1.id         > sb2.id;

ALTER TABLE student_batch
    ADD CONSTRAINT uk_student_batch UNIQUE (student_id, batch_id);


-- ============================================================================
-- 8. NAYI TABLE : approval_requests
-- ============================================================================
CREATE TABLE approval_requests (
    id                      BIGINT       NOT NULL AUTO_INCREMENT,
    request_type            VARCHAR(30)  NOT NULL,
    status                  VARCHAR(20)  NOT NULL DEFAULT 'PENDING',

    requested_by_trainer_id BIGINT       NOT NULL,
    reviewed_by_admin_id    BIGINT       NULL,

    batch_id                BIGINT       NULL,
    student_id              BIGINT       NULL,

    request_note            TEXT         NULL,
    admin_remark            TEXT         NULL,
    reviewed_at             DATETIME     NULL,

    created_at              DATETIME     NULL,
    updated_at              DATETIME     NULL,

    PRIMARY KEY (id),

    CONSTRAINT fk_ar_trainer FOREIGN KEY (requested_by_trainer_id) REFERENCES trainers (id),
    CONSTRAINT fk_ar_admin   FOREIGN KEY (reviewed_by_admin_id)    REFERENCES admins (id),
    CONSTRAINT fk_ar_batch   FOREIGN KEY (batch_id)                REFERENCES batches (id),
    CONSTRAINT fk_ar_student FOREIGN KEY (student_id)              REFERENCES students (id)
) ENGINE = InnoDB;

-- Admin dashboard : "saare PENDING requests dikhao"
CREATE INDEX idx_approval_status_type ON approval_requests (status, request_type);


-- ============================================================================
-- 9. NAYI TABLE : group_messages
-- ============================================================================
CREATE TABLE group_messages (
    id                   BIGINT       NOT NULL AUTO_INCREMENT,
    batch_id             BIGINT       NOT NULL,

    message_type         VARCHAR(20)  NOT NULL,
    message              TEXT         NULL,

    file_url             TEXT         NULL,
    cloudinary_public_id VARCHAR(255) NULL,
    file_name            VARCHAR(255) NULL,
    file_size            BIGINT       NULL,

    sender_type          VARCHAR(20)  NOT NULL,
    sender_student_id    BIGINT       NULL,
    sender_trainer_id    BIGINT       NULL,
    sender_admin_id      BIGINT       NULL,

    expires_at           DATETIME     NOT NULL,
    deleted              BOOLEAN      NOT NULL DEFAULT FALSE,
    reply_to_id          BIGINT       NULL,

    created_at           DATETIME     NOT NULL,

    PRIMARY KEY (id),

    CONSTRAINT fk_gm_batch    FOREIGN KEY (batch_id)          REFERENCES batches (id),
    CONSTRAINT fk_gm_student  FOREIGN KEY (sender_student_id) REFERENCES students (id),
    CONSTRAINT fk_gm_trainer  FOREIGN KEY (sender_trainer_id) REFERENCES trainers (id),
    CONSTRAINT fk_gm_admin    FOREIGN KEY (sender_admin_id)   REFERENCES admins (id),
    CONSTRAINT fk_gm_reply_to FOREIGN KEY (reply_to_id)       REFERENCES group_messages (id)
) ENGINE = InnoDB;

-- Chat kholte hi latest messages (sabse zyada chalne wali query)
CREATE INDEX idx_gm_batch_created ON group_messages (batch_id, created_at);

-- Scheduler : "7 din purane messages dhundo"
CREATE INDEX idx_gm_expires ON group_messages (expires_at);