package com.uncodemy.lms.dto.response;

import com.uncodemy.lms.model.ApprovalRequest;
import com.uncodemy.lms.model.enums.ApprovalStatus;
import com.uncodemy.lms.model.enums.RequestType;

import lombok.*;

import java.time.LocalDateTime;

/**
 * ============================================================================
 * ApprovalRequestResponse   ---  Admin ke dashboard ka OUTPUT
 * ============================================================================
 *
 * Sample JSON:
 * ---------------------------------------------------------------------------
 * {
 *   "id"           : 5,
 *   "requestType"  : "BATCH_CREATE",
 *   "status"       : "PENDING",
 *   "trainerId"    : "TR101",
 *   "trainerName"  : "Rahul Sharma",
 *   "batchId"      : "JAVA102",
 *   "batchName"    : "Java Advanced",
 *   "studentId"    : null,
 *   "studentName"  : null,
 *   "requestNote"  : "Naya evening batch shuru karna hai",
 *   "createdAt"    : "2026-07-31 18:30:00"
 * }
 *
 * STUDENT WALE FIELDS NULL KYUN?
 * ---------------------------------------------------------------------------
 * BATCH_CREATE request me student hota hi nahi.
 *
 *   BATCH_CREATE -> batch bhara, student null
 *   STUDENT_ADD  -> dono bhare (student kis batch me add ho raha)
 *
 * Frontend requestType dekh ke decide kar lega ki
 * kya dikhana hai.
 * ============================================================================
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalRequestResponse {

    private Long id;

    /** BATCH_CREATE / STUDENT_ADD */
    private RequestType requestType;

    /** PENDING / APPROVED / REJECTED */
    private ApprovalStatus status;

    // ---- Kisne bheji ----
    private String trainerId;
    private String trainerName;
    private String trainerEmail;

    // ---- Target Batch ----
    private String batchId;
    private String batchName;

    // ---- Target Student (sirf STUDENT_ADD me) ----
    private String studentId;
    private String studentName;

    /** Trainer ka note */
    private String requestNote;

    /** Admin ka jawab (reject me reason) */
    private String adminRemark;

    /** Kis admin ne review kiya */
    private String reviewedByAdminId;

    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;


    // ========================================================================
    // MAPPER
    // ========================================================================
    /**
     * ⚠️ Ye method 4 LAZY relations touch karta hai:
     *    requestedByTrainer, batch, student, reviewedByAdmin
     *
     * Isliye list banate waqt repository ki
     * "findByStatusWithDetails" (JOIN FETCH wali) use karna —
     * warna 20 requests ke liye 80 query chal jayengi.
     */
    public static ApprovalRequestResponse from(ApprovalRequest ar) {

        if (ar == null) {
            return null;
        }

        var builder = ApprovalRequestResponse.builder()
                .id(ar.getId())
                .requestType(ar.getRequestType())
                .status(ar.getStatus())
                .requestNote(ar.getRequestNote())
                .adminRemark(ar.getAdminRemark())
                .reviewedAt(ar.getReviewedAt())
                .createdAt(ar.getCreatedAt());

        // Trainer (hamesha hota hai, phir bhi null-check safe hai)
        if (ar.getRequestedByTrainer() != null) {
            builder.trainerId(ar.getRequestedByTrainer().getTrainerId())
                   .trainerName(ar.getRequestedByTrainer().getName())
                   .trainerEmail(ar.getRequestedByTrainer().getEmail());
        }

        // Batch (dono request type me hota hai)
        if (ar.getBatch() != null) {
            builder.batchId(ar.getBatch().getBatchId())
                   .batchName(ar.getBatch().getBatchName());
        }

        // Student (sirf STUDENT_ADD me)
        if (ar.getStudent() != null) {
            builder.studentId(ar.getStudent().getStudentId())
                   .studentName(ar.getStudent().getName());
        }

        // Admin (sirf review hone ke baad)
        if (ar.getReviewedByAdmin() != null) {
            builder.reviewedByAdminId(ar.getReviewedByAdmin().getAdminId());
        }

        return builder.build();
    }
}