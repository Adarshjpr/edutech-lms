package com.uncodemy.lms.repository;

import com.uncodemy.lms.model.Batch;
import com.uncodemy.lms.model.Student;
import com.uncodemy.lms.model.StudentBatch;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * ============================================================================
 * StudentBatchRepository   ---  Enrollment table
 * ============================================================================
 *
 * Ye table batao ki KAUN SA STUDENT KIS BATCH me hai.
 *
 * API 5  -> student ko batch me add
 * API 8  -> announcement kis-kis ko jayega
 * API 12 -> group me kaun-kaun hai
 *
 * "active" FLAG KA MATLAB
 * ---------------------------------------------------------------------------
 * Student ne batch chhod diya to row DELETE nahi karte,
 * bas active = false kar dete hain.
 *
 * Isse enrollment history bachi rehti hai.
 * Lekin har query me active = true ka filter lagana ZAROORI hai,
 * warna chhode hue students ko bhi mail chali jayegi.
 * ============================================================================
 */
@Repository
public interface StudentBatchRepository extends JpaRepository<StudentBatch, Long> {

    // ========================================================================
    // DUPLICATE ENROLLMENT CHECK
    // ========================================================================

    /**
     * Student iss batch me pehle se hai?
     *
     * DB me unique constraint bhi laga hai (Phase 0 me),
     * lekin pehle check karne se user ko saaf message
     * milta hai — DB error ka technical message nahi.
     */
    boolean existsByStudentAndBatch(Student student, Batch batch);

    /**
     * Enrollment record nikalo (active ho ya na ho).
     *
     * KAB CHAHIYE
     * -----------------------------------------------------------------------
     * Student ne batch chhoda tha (active = false), ab wapas
     * join kar raha hai.
     *
     * Nayi row banate to unique constraint tootegi.
     * Isliye purani row dhundh ke active = true kar dete hain.
     */
    Optional<StudentBatch> findByStudentAndBatch(Student student, Batch batch);


    // ========================================================================
    // BATCH KE STUDENTS
    // ========================================================================

    /**
     * Batch ke active enrollments — student ki details ke saath.
     *
     * JOIN FETCH se N+1 problem nahi hoti.
     */
    @Query(value = """
            SELECT sb FROM StudentBatch sb
            JOIN FETCH sb.student s
            WHERE sb.batch.batchId = :batchId
              AND sb.active = true
              AND s.approvalStatus = com.uncodemy.lms.model.enums.ApprovalStatus.APPROVED
            """,
            countQuery = """
            SELECT COUNT(sb) FROM StudentBatch sb
            WHERE sb.batch.batchId = :batchId AND sb.active = true
            """)
    Page<StudentBatch> findActiveByBatchId(@Param("batchId") String batchId, Pageable pageable);


    // ========================================================================
    // STUDENT KE BATCHES
    // ========================================================================

    /**
     * Ek student kis-kis batch me hai.
     *
     * StudentResponse me batch ki list dikhane ke liye.
     */
    @Query("""
           SELECT sb FROM StudentBatch sb
           JOIN FETCH sb.batch b
           LEFT JOIN FETCH b.trainer
           WHERE sb.student.studentId = :studentId
             AND sb.active = true
           """)
    List<StudentBatch> findActiveByStudentId(@Param("studentId") String studentId);


    // ========================================================================
    // COUNT  --- BatchResponse ke totalStudents ke liye
    // ========================================================================

    /**
     * Batch me kitne active students hain.
     *
     * batch.getStudentBatches().size() NAHI karna —
     * wo poori list memory me load kar deta hai.
     */
    long countByBatchAndActiveTrue(Batch batch);

    @Query("""
           SELECT COUNT(sb) FROM StudentBatch sb
           WHERE sb.batch.batchId = :batchId AND sb.active = true
           """)
    long countActiveByBatchId(@Param("batchId") String batchId);

    /** Student kitne batches me hai */
    long countByStudentAndActiveTrue(Student student);
}