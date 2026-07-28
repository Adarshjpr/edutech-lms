package com.uncodemy.lms.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

/**
 * ============================================================================
 * StudentBatch Entity
 * ============================================================================
 *
 * Ye Entity Student aur Batch ke beech ke relationship (Enrollment)
 * ko represent karti hai.
 *
 * Direct @ManyToMany use karne ke bajay humne alag Entity banayi hai,
 * taaki future me enrollment se related extra information aasani se
 * store ki ja sake.
 *
 * Current Database Structure
 * ---------------------------------------------------------------------------
 *
 * student_batch
 * ---------------------------------------------------------------------------
 * id | student_id | batch_id | joined_at
 * ---------------------------------------------------------------------------
 * 1  | STU101     | JAVA101  | 2026-07-28 10:30
 * 2  | STU101     | MERN201  | 2026-08-01 09:00
 * 3  | STU102     | JAVA101  | 2026-08-05 11:15
 *
 * Relationships
 * ---------------------------------------------------------------------------
 *
 * Student (1)  --------<  StudentBatch  >-------- (1) Batch
 *
 * Ek Student multiple batches join kar sakta hai.
 * Ek Batch me multiple students enroll ho sakte hain.
 *
 * Har enrollment ka ek alag StudentBatch record hoga.
 *
 * Example:
 *
 * Student : Adarsh
 *
 *      |
 *      |-----------------------------|
 *      |                             |
 * StudentBatch                  StudentBatch
 *      |                             |
 *      ▼                             ▼
 * Java Batch                    MERN Batch
 *
 * Benefits
 * ---------------------------------------------------------------------------
 * ✔ Production Ready Design
 * ✔ Clean Database Structure
 * ✔ Future Scalability
 * ✔ Enrollment History Maintain Kar Sakte Hain
 *
 * Future me isi Entity me easily ye fields add kiye ja sakte hain:
 *
 * - Certificate Status
 * - Progress Percentage
 * - Attendance
 * - Payment Status
 * - Completion Date
 * - Remarks
 * - Assignment Score
 *
 * Is design ko industry me Enrollment Table bhi kaha jata hai.
 * ============================================================================
 */
@Entity
@Table(name = "student_batch")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentBatch {

    /**
     * Primary Key
     *
     * Database automatically unique ID generate karega.
     *
     * Example:
     * 1
     * 2
     * 3
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Student Reference (Foreign Key)
     *
     * Ye field students table ko refer karti hai.
     *
     * Ek Student ke multiple StudentBatch records ho sakte hain,
     * lekin ek StudentBatch record sirf ek Student se belong karega.
     *
     * FetchType.LAZY
     * Student ki information tabhi load hogi jab zarurat hogi.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    /**
     * Batch Reference (Foreign Key)
     *
     * Ye field batches table ko refer karti hai.
     *
     * Ek Batch ke multiple StudentBatch records ho sakte hain,
     * lekin ek StudentBatch record sirf ek Batch se belong karega.
     *
     * FetchType.LAZY
     * Batch ki information tabhi load hogi jab access ki jayegi.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    private Batch batch;

    /**
     * Student Join Date
     *
     * Is field me store hoga ki student ne
     * is batch ko kab join kiya tha.
     *
     * Example:
     * 2026-07-28T10:30:00
     */
    private LocalDateTime joinedAt;


    @CreatedDate
private LocalDateTime createdAt;

@LastModifiedDate
private LocalDateTime updatedAt;
}