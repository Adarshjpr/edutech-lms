package com.uncodemy.lms.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.uncodemy.lms.model.enums.ApprovalStatus;
import com.uncodemy.lms.model.enums.RequestType;

/**
 * ============================================================================
 * ApprovalRequest Entity   (NAYI)
 * ============================================================================
 *
 * Jab bhi Trainer koi aisa kaam kare jiske liye Admin ki
 * permission chahiye, uski ek entry yahan banegi.
 *
 * API 4 --- Trainer batch create kare  -> BATCH_CREATE request
 * API 7 --- Trainer student add kare   -> STUDENT_ADD request
 *
 * Flow:
 * ---------------------------------------------------------------------------
 *
 *   Trainer batch banata hai
 *            |
 *            v
 *   Batch table me row bani (approvalStatus = PENDING)
 *            |
 *            v
 *   ApprovalRequest row bani (status = PENDING)
 *            |
 *            v
 *   Admin ke dashboard pe dikha
 *            |
 *      +-----+-----+
 *      |           |
 *   APPROVE     REJECT
 *      |           |
 *      v           v
 *  Batch ka     Batch ka
 *  status =     status =
 *  APPROVED     REJECTED
 *
 * ---------------------------------------------------------------------------
 * Alag-alag table (BatchRequest, StudentRequest) kyun nahi banayi?
 *
 * Kyunki tab har naye approval ke liye nayi table, naya repository,
 * naya controller banana padta. Ek hi table me RequestType se
 * kaam ho jayega, aur Admin ko ek hi jagah saare pending
 * requests mil jayenge.
 * ============================================================================
 */
@Entity
@Table(
    name = "approval_requests",
    indexes = {
        // Admin ka dashboard: "saare PENDING requests dikhao" — ye query fast hogi
        @Index(name = "idx_approval_status_type", columnList = "status, request_type")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class ApprovalRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Kis cheez ki request hai.
     *
     * BATCH_CREATE -> trainer ne naya batch banaya
     * STUDENT_ADD  -> trainer ne naya student add kiya
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "request_type", nullable = false)
    private RequestType requestType;

    /**
     * Request ka current status.
     *
     * PENDING  -> admin ne abhi dekha nahi
     * APPROVED -> admin ne haan kar di
     * REJECTED -> admin ne mana kar diya
     */
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private ApprovalStatus status = ApprovalStatus.PENDING;

    /**
     * Kis Trainer ne request bheji.
     * Ye hamesha set hoga (request trainer hi bhejta hai).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by_trainer_id", nullable = false)
    private Trainer requestedByTrainer;

    /**
     * Kis Admin ne approve / reject kiya.
     * PENDING state me null rahega.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_admin_id")
    private Admin reviewedByAdmin;

    /**
     * Target Batch
     *
     * requestType = BATCH_CREATE -> naya bana hua batch
     * requestType = STUDENT_ADD  -> student kis batch me add ho raha hai
     *
     * Dono case me set rahega.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id")
    private Batch batch;

    /**
     * Target Student
     *
     * Sirf STUDENT_ADD ke case me set hoga.
     * BATCH_CREATE me null.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private Student student;

    /**
     * Trainer ka note.
     *
     * Example: "Ye student walk-in aaya tha, urgent add karna hai"
     */
    @Column(columnDefinition = "TEXT")
    private String requestNote;

    /**
     * Admin ka jawab.
     *
     * REJECT karte waqt reason dena zaroori hoga
     * taaki trainer ko pata chale kyun mana kiya.
     *
     * Example: "Is naam ka batch already chal raha hai"
     */
    @Column(columnDefinition = "TEXT")
    private String adminRemark;

    /**
     * Admin ne kab review kiya.
     * PENDING me null.
     */
    private LocalDateTime reviewedAt;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}