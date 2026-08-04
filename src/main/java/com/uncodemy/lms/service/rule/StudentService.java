package com.uncodemy.lms.service.rule;

import com.uncodemy.lms.dto.request.StudentCreateRequest;
import com.uncodemy.lms.dto.request.StudentEnrollRequest;
import com.uncodemy.lms.dto.response.StudentResponse;
import com.uncodemy.lms.model.Student;
import com.uncodemy.lms.model.enums.ApprovalStatus;
import com.uncodemy.lms.model.enums.StudentStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * ============================================================================
 * StudentService  (Interface)
 * ============================================================================
 *
 * API 5 -> batch me student add (admin direct / trainer approval)
 * API 7 -> naya student create (admin direct / trainer approval)
 * ============================================================================
 */
public interface StudentService {

    // ========================================================================
    // API 7 --- CREATE
    // ========================================================================

    /**
     * Admin naya student banata hai — SEEDHA APPROVED.
     *
     * batchId diya ho to enrollment bhi ho jayegi
     * aur welcome mail bhi jayegi.
     */
    StudentResponse createByAdmin(StudentCreateRequest request, String adminId);

    /**
     * Trainer naya student banata hai — PENDING.
     *
     *   1. Student banega (approvalStatus = PENDING)
     *   2. ApprovalRequest banegi (STUDENT_ADD)
     *   3. Admins ko mail
     *   4. Student ko KOI MAIL NAHI (approve hone ke baad jayegi)
     *
     * batchId diya ho to wo batch trainer ka hona chahiye.
     */
    StudentResponse createByTrainer(StudentCreateRequest request, String trainerId, String note);


    // ========================================================================
    // API 5 --- BATCH ME ADD  (pehle se bana student)
    // ========================================================================

    /**
     * Admin student ko batch me daalta hai — turant.
     */
    StudentResponse enrollByAdmin(StudentEnrollRequest request, String adminId);

    /**
     * Trainer student ko apne batch me daalta hai — approval ke baad.
     */
    StudentResponse enrollByTrainer(StudentEnrollRequest request, String trainerId);

    /**
     * Student ko batch se hatao (soft — active = false).
     */
    void removeFromBatch(String studentId, String batchId);


    // ========================================================================
    // READ
    // ========================================================================

    /** Ek student — batch ki list ke saath */
    StudentResponse getByStudentId(String studentId);

    /** Saare students, approval status ke hisaab se */
    Page<StudentResponse> getAllStudents(ApprovalStatus approvalStatus,
                                         StudentStatus status,
                                         Pageable pageable);

    /** Ek batch ke students */
    Page<StudentResponse> getStudentsByBatch(String batchId, Pageable pageable);

    /** Naam / email / course se search */
    Page<StudentResponse> searchStudents(String query, Pageable pageable);


    // ========================================================================
    // UPDATE
    // ========================================================================

    StudentResponse updateStatus(String studentId, StudentStatus status);


    // ========================================================================
    // INTERNAL
    // ========================================================================

    Student getEntityOrThrow(String studentId);
}