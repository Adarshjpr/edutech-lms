package com.uncodemy.lms.repository;

import com.uncodemy.lms.model.Student;
import com.uncodemy.lms.model.enums.ApprovalStatus;
import com.uncodemy.lms.model.enums.StudentStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * ============================================================================
 * StudentRepository
 * ============================================================================
 *
 * API 5 -> batch me student add
 * API 7 -> student create (admin direct / trainer approval ke saath)
 *
 * approvalStatus KA FILTER
 * ---------------------------------------------------------------------------
 * Trainer ka add kiya student PENDING hota hai.
 * Normal list me wo nahi dikhna chahiye — warna admin ko
 * lagega student add ho gaya, jabki approve hua hi nahi.
 * ============================================================================
 */
@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    // ========================================================================
    // FIND
    // ========================================================================

    Optional<Student> findByStudentId(String studentId);

    Optional<Student> findByEmail(String email);

    /**
     * Student + jo trainer ne add kiya tha, ek hi query me.
     * Response me "addedBy" dikhane ke liye.
     */
    @Query("SELECT s FROM Student s LEFT JOIN FETCH s.addedByTrainer WHERE s.studentId = :studentId")
    Optional<Student> findByStudentIdWithTrainer(@Param("studentId") String studentId);


    // ========================================================================
    // EXISTS  --- duplicate check
    // ========================================================================

    boolean existsByEmail(String email);

    boolean existsByStudentId(String studentId);


    // ========================================================================
    // LAST ID  --- STU101, STU102...
    // ========================================================================
    @Query("SELECT s.studentId FROM Student s ORDER BY s.id DESC LIMIT 1")
    Optional<String> findLastStudentId();


    // ========================================================================
    // LIST
    // ========================================================================

    /**
     * Approval status ke hisaab se.
     *
     * APPROVED -> normal list
     * PENDING  -> jo abhi approve nahi hue
     */
    Page<Student> findByApprovalStatus(ApprovalStatus approvalStatus, Pageable pageable);

    /**
     * Approval + student status dono.
     * Example: APPROVED + ACTIVE -> abhi padh rahe students
     */
    Page<Student> findByApprovalStatusAndStatus(
            ApprovalStatus approvalStatus, StudentStatus status, Pageable pageable);


    // ========================================================================
    // SEARCH
    // ========================================================================
    /**
     * Naam / email / course — teeno me se kahin bhi match.
     *
     * ":q IS NULL OR ..." wala trick — parameter null hai
     * to filter lagta hi nahi.
     */
    @Query("""
           SELECT s FROM Student s
           WHERE s.approvalStatus = com.uncodemy.lms.model.enums.ApprovalStatus.APPROVED
             AND (:q IS NULL OR
                  LOWER(s.name)   LIKE LOWER(CONCAT('%', :q, '%')) OR
                  LOWER(s.email)  LIKE LOWER(CONCAT('%', :q, '%')) OR
                  LOWER(s.course) LIKE LOWER(CONCAT('%', :q, '%')))
           """)
    Page<Student> searchStudents(@Param("q") String query, Pageable pageable);


    // ========================================================================
    // COUNT
    // ========================================================================

    long countByApprovalStatus(ApprovalStatus approvalStatus);

    long countByApprovalStatusAndStatus(ApprovalStatus approvalStatus, StudentStatus status);
}