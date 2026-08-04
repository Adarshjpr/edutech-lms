package com.uncodemy.lms.service.rule;

import com.uncodemy.lms.dto.request.ApprovalActionRequest;
import com.uncodemy.lms.dto.response.ApprovalRequestResponse;
import com.uncodemy.lms.model.enums.ApprovalStatus;
import com.uncodemy.lms.model.enums.RequestType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * ============================================================================
 * ApprovalService  (Interface)
 * ============================================================================
 *
 * Trainer ki requests ko Admin approve / reject karta hai.
 *
 * API 4 ka doosra hissa (batch approval)
 * API 7 ka doosra hissa (student approval) --- Phase 4 me complete hoga
 *
 * EK HI SERVICE DONO KE LIYE
 * ---------------------------------------------------------------------------
 * BATCH_CREATE aur STUDENT_ADD dono ek hi table me hain,
 * isliye ek hi service dono handle karegi.
 *
 * Farak sirf approve() ke andar hai — requestType dekh ke
 * alag-alag kaam hota hai.
 * ============================================================================
 */
public interface ApprovalService {

    // ========================================================================
    // ADMIN ACTIONS
    // ========================================================================

    /**
     * Request APPROVE karo.
     *
     * BATCH_CREATE me kya hoga:
     *   1. request.status = APPROVED
     *   2. batch.approvalStatus = APPROVED
     *   3. batch.createdByAdmin = approve karne wala admin
     *   4. Trainer ko mail
     *
     * STUDENT_ADD me kya hoga (Phase 4):
     *   1. request.status = APPROVED
     *   2. student.approvalStatus = APPROVED
     *   3. StudentBatch (enrollment) banegi
     *   4. Student ko welcome mail
     *   5. Trainer ko confirmation mail
     *
     * @throws com.uncodemy.lms.exception.BadRequestException
     *         agar request pehle se review ho chuki hai
     */
    ApprovalRequestResponse approve(Long requestId, ApprovalActionRequest action);

    /**
     * Request REJECT karo.
     *
     *   1. request.status = REJECTED
     *   2. target (batch/student) ka status bhi REJECTED
     *   3. Trainer ko mail --- REASON ke saath
     *
     * remark ZAROORI hai — bina reason ke reject nahi hoga.
     */
    ApprovalRequestResponse reject(Long requestId, ApprovalActionRequest action);


    // ========================================================================
    // ADMIN DASHBOARD
    // ========================================================================

    /**
     * Saari requests, status ke hisaab se.
     *
     * @param status      PENDING / APPROVED / REJECTED
     * @param requestType optional filter (BATCH_CREATE / STUDENT_ADD)
     */
    Page<ApprovalRequestResponse> getRequests(ApprovalStatus status,
                                              RequestType requestType,
                                              Pageable pageable);

    /**
     * Ek request ki details.
     */
    ApprovalRequestResponse getById(Long requestId);

    /**
     * Kitni requests pending hain.
     *
     * Dashboard pe badge dikhane ke liye : "Approvals (3)"
     */
    long countPending();


    // ========================================================================
    // TRAINER DASHBOARD
    // ========================================================================

    /**
     * Trainer ki apni requests.
     *
     * "Maine kya bheja tha aur uska kya hua"
     *
     * @param status optional — null bheja to sab aayengi
     */
    Page<ApprovalRequestResponse> getMyRequests(String trainerId,
                                                ApprovalStatus status,
                                                Pageable pageable);
}