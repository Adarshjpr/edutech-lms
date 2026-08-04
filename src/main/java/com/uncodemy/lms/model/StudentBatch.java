package com.uncodemy.lms.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * ============================================================================
 * StudentBatch Entity  (Enrollment Table)
 * ============================================================================
 *
 * Student aur Batch ke beech ka relationship.
 *
 * Direct @ManyToMany ke bajay alag Entity isliye banayi
 * taaki enrollment se related extra info store ho sake.
 *
 * student_batch
 * ---------------------------------------------------------
 * id | student_id | batch_id | joined_at
 * ---------------------------------------------------------
 * 1  | STU101     | JAVA101  | 2026-07-28 10:30
 * 2  | STU101     | MERN201  | 2026-08-01 09:00
 * 3  | STU102     | JAVA101  | 2026-08-05 11:15
 *
 * Ek Student multiple batches join kar sakta hai.
 * Ek Batch me multiple students enroll ho sakte hain.
 *
 * API 12 (Group Chat) ke liye bhi YAHI table source of truth hai —
 * "jo student iss batch me hai, wahi group me hoga".
 * ============================================================================
 */
@Entity
@Table(
    name = "student_batch",
    uniqueConstraints = {
        /**
         * NAYA --- BAHUT ZAROORI
         *
         * Ek student ek hi batch me DO BAAR enroll na ho.
         *
         * Pehle ye constraint nahi tha, isliye galti se
         * do baar add karne pe duplicate row ban jati thi,
         * aur announcement ka mail bhi 2 baar jaata.
         */
        @UniqueConstraint(
            name = "uk_student_batch",
            columnNames = {"student_id", "batch_id"}
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class StudentBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Student Reference (Foreign Key)
     *
     * Ek Student ke multiple StudentBatch records ho sakte hain,
     * lekin ek record sirf ek Student ka hoga.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    /**
     * Batch Reference (Foreign Key)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    private Batch batch;

    /**
     * Student ne ye batch kab join kiya.
     * Example: 2026-07-28T10:30:00
     */
    @Column(nullable = false)
    private LocalDateTime joinedAt;

    /**
     * NAYA FIELD
     *
     * Student ne batch kab chhoda (agar chhoda ho).
     * Normally null rahega.
     */
    private LocalDateTime leftAt;

    /**
     * NAYA FIELD
     *
     * Enrollment abhi chalu hai ya nahi.
     *
     * Row delete karne ke bajay ise false karenge,
     * taaki enrollment history maintain rahe.
     *
     * false hone par:
     * - announcement ka mail nahi jayega
     * - group chat me nahi dikhega
     */
    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}