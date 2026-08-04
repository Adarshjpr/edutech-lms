package com.uncodemy.lms.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.uncodemy.lms.model.enums.BatchStatus;
import com.uncodemy.lms.model.enums.ApprovalStatus;

/**
 * Batch Entity
 *
 * Ye class database ke "batches" table ko represent karti hai.
 *
 * Example:
 * Batch ID   : JAVA101
 * Batch Name : Java Full Stack
 * Timing     : 7:00 PM - 9:00 PM
 */
@Entity
@Table(name = "batches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Batch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Batch ka unique ID.
     * Example: JAVA101, MERN201
     *
     * API 12 me isi ID pe group bana hoga.
     */
    @Column(name = "batch_id", unique = true, nullable = false)
    private String batchId;

    /**
     * Batch ka naam.
     * Example: Java Full Stack
     */
    @Column(nullable = false)
    private String batchName;

    /**
     * Batch ka timing.
     * Example: 7 PM - 9 PM
     */
    private String timing;

    /**
     * Online Class Meeting Link (Google Meet / Zoom).
     * TEXT kyunki URL lamba ho sakta hai.
     */
    @Column(columnDefinition = "TEXT")
    private String meetLink;

    /**
     * Certificate Download Link.
     */
    @Column(columnDefinition = "TEXT")
    private String certificateLink;

    /**
     * Community / WhatsApp group ka external link (agar ho).
     */
    @Column(columnDefinition = "TEXT")
    private String communityLink;

    /**
     * API 6 --- Batch me abhi kaunsa topic chal raha hai.
     *
     * Admin ya Trainer dono update kar sakte hain.
     * API 10 me isi field pe search hoga.
     *
     * Example: Spring Boot, React Hooks
     */
    private String currentTopic;

    /**
     * NAYA FIELD
     *
     * Current topic last kab update hua tha.
     * Search result me "kitna purana topic hai" dikhane ke kaam aayega.
     */
    private LocalDateTime topicUpdatedAt;

    /**
     * NAYE FIELDS
     *
     * Batch kab shuru hua / kab khatam hoga.
     * Certificate aur status automation me kaam aayega.
     */
    private LocalDate startDate;
    private LocalDate endDate;

    /**
     * Batch ka current status.
     * Example: UPCOMING, ACTIVE, COMPLETED
     */
    @Enumerated(EnumType.STRING)
    private BatchStatus status;

    /**
     * NAYA FIELD  --- API 4 ki core requirement
     *
     * Admin ne banaya   -> APPROVED (turant chalu)
     * Trainer ne banaya -> PENDING  (admin approve karega)
     *
     * PENDING batch me na student add hoga,
     * na announcement jayega.
     */
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private ApprovalStatus approvalStatus = ApprovalStatus.APPROVED;

    /**
     * Many-to-One
     *
     * Batch ka assigned Trainer (jo padhayega).
     *
     * Dhyan do: ye "createdByTrainer" se alag hai.
     * Admin batch bana kar kisi bhi trainer ko assign kar sakta hai.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trainer_id")
    private Trainer trainer;

    /**
     * NAYA FIELD
     *
     * Kis Trainer ne create request bheji thi.
     * Admin ne banaya to null rahega.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_trainer_id")
    private Trainer createdByTrainer;

    /**
     * NAYA FIELD
     *
     * Kis Admin ne banaya / approve kiya.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_admin_id")
    private Admin createdByAdmin;

    /**
     * NAYA RELATION  --- pehle sirf comment tha, actual field missing thi
     *
     * Batch me enrolled students (StudentBatch ke through).
     *
     * HashSet use kiya hai taaki duplicate enrollment na aaye.
     *
     * JAVA101 Batch
     *    |---- STU101
     *    |---- STU102
     */
    @Builder.Default
    @OneToMany(mappedBy = "batch", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<StudentBatch> studentBatches = new HashSet<>();

    /**
     * One-to-Many
     * Ek Batch me multiple Announcements ho sakte hain.
     */
    @Builder.Default
    @OneToMany(mappedBy = "batch", cascade = CascadeType.ALL)
    private List<Announcement> announcements = new ArrayList<>();

    /**
     * NAYA RELATION  --- API 9
     *
     * Batch ke saare study contents.
     * Content entity me ab "batch" field add ho rahi hai.
     */
    @Builder.Default
    @OneToMany(mappedBy = "batch")
    private List<Content> contents = new ArrayList<>();

    // NOTE: GroupMessage ka relation Phase 9 me add karenge,
    //       kyunki wo entity abhi bani hi nahi hai.

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}