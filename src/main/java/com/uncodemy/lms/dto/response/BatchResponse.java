package com.uncodemy.lms.dto.response;

import com.uncodemy.lms.model.Batch;
import com.uncodemy.lms.model.enums.ApprovalStatus;
import com.uncodemy.lms.model.enums.BatchStatus;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * ============================================================================
 * BatchResponse   ---  Batch ka OUTPUT
 * ============================================================================
 *
 * Sample JSON:
 * ---------------------------------------------------------------------------
 * {
 *   "id"            : 1,
 *   "batchId"       : "JAVA101",
 *   "batchName"     : "Java Full Stack",
 *   "timing"        : "7 PM - 9 PM",
 *   "currentTopic"  : "Spring Boot Basics",
 *   "status"        : "ACTIVE",
 *   "approvalStatus": "APPROVED",
 *   "trainerId"     : "TR101",
 *   "trainerName"   : "Rahul Sharma",
 *   "totalStudents" : 24
 * }
 *
 * ENTITY ME NESTED OBJECT HAI, YAHAN FLAT HAI
 * ---------------------------------------------------------------------------
 * Entity me : batch.getTrainer().getName()
 * DTO me    : trainerName
 *
 * Nested Trainer object bhejne se do problem hoti:
 *   1. Trainer ke andar batches ki list -> infinite loop
 *   2. Trainer ka poora data bekaar me jata (password hash bhi!)
 *
 * Flat rakhne se sirf jo chahiye wahi jata hai.
 * ============================================================================
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatchResponse {

    private Long id;
    private String batchId;
    private String batchName;
    private String timing;

    private String meetLink;
    private String communityLink;
    private String certificateLink;

    /** API 6 --- abhi kya padha raha hai */
    private String currentTopic;

    /** Topic last kab update hua (API 10 me kaam aayega) */
    private LocalDateTime topicUpdatedAt;

    private LocalDate startDate;
    private LocalDate endDate;

    /** UPCOMING / ACTIVE / COMPLETED */
    private BatchStatus status;

    /**
     * PENDING / APPROVED / REJECTED
     *
     * Trainer ke banaye batch me ye PENDING hoga
     * jab tak admin approve na kare.
     */
    private ApprovalStatus approvalStatus;

    // ---- Assigned Trainer (flat) ----
    private String trainerId;
    private String trainerName;

    /** Kisne banaya tha — "TR101" ya "ADM101" */
    private String createdBy;

    /** Iss batch me kitne active students hain */
    private Integer totalStudents;

    private LocalDateTime createdAt;


    // ========================================================================
    // MAPPER
    // ========================================================================
    /**
     * ⚠️ ZAROORI BAAT — LAZY LOADING
     * -----------------------------------------------------------------------
     * Ye method batch.getTrainer().getName() call karta hai.
     *
     * Trainer LAZY hai, matlab ye call DB me EXTRA QUERY maar sakti hai.
     *
     * Isliye ye method HAMESHA @Transactional ke andar
     * chalna chahiye (service layer me), warna
     * LazyInitializationException aayegi.
     *
     * List banate waqt N+1 se bachne ke liye repository me
     * "findByBatchIdWithTrainer" jaisi JOIN FETCH query use karo.
     *
     * @param totalStudents service alag se count karke degi
     */
    public static BatchResponse from(Batch batch, Integer totalStudents) {

        if (batch == null) {
            return null;
        }

        // Trainer null ho sakta hai (batch bana lekin assign nahi hua)
        String trainerId   = (batch.getTrainer() != null) ? batch.getTrainer().getTrainerId() : null;
        String trainerName = (batch.getTrainer() != null) ? batch.getTrainer().getName() : null;

        // Kisne banaya — trainer ya admin
        String createdBy = null;
        if (batch.getCreatedByTrainer() != null) {
            createdBy = batch.getCreatedByTrainer().getTrainerId();
        } else if (batch.getCreatedByAdmin() != null) {
            createdBy = batch.getCreatedByAdmin().getAdminId();
        }

        return BatchResponse.builder()
                .id(batch.getId())
                .batchId(batch.getBatchId())
                .batchName(batch.getBatchName())
                .timing(batch.getTiming())
                .meetLink(batch.getMeetLink())
                .communityLink(batch.getCommunityLink())
                .certificateLink(batch.getCertificateLink())
                .currentTopic(batch.getCurrentTopic())
                .topicUpdatedAt(batch.getTopicUpdatedAt())
                .startDate(batch.getStartDate())
                .endDate(batch.getEndDate())
                .status(batch.getStatus())
                .approvalStatus(batch.getApprovalStatus())
                .trainerId(trainerId)
                .trainerName(trainerName)
                .createdBy(createdBy)
                .totalStudents(totalStudents)
                .createdAt(batch.getCreatedAt())
                .build();
    }

    /** Shortcut — student count ki zarurat na ho to */
    public static BatchResponse from(Batch batch) {
        return from(batch, null);
    }
}