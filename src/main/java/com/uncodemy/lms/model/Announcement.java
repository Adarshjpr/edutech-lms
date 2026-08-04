package com.uncodemy.lms.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.uncodemy.lms.model.enums.AnnouncementScope;

/**
 * Announcement Entity
 *
 * Batch, Trainer ya Admin ke dwara bheje gaye announcements.
 *
 * Example:
 * ------------------------------------------
 * Title   : Holiday Notice
 * Message : Aaj ki class cancel hai.
 * Scope   : SPECIFIC_BATCH
 * Batch   : JAVA101
 * ------------------------------------------
 */
@Entity
@Table(
    name = "announcements",
    indexes = {
        // batch ke announcements latest-first nikalne ke liye
        @Index(name = "idx_ann_batch_created", columnList = "batch_id, created_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Announcement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * NAYA FIELD
     *
     * Announcement ka title / heading.
     *
     * Mail ka SUBJECT yahi banega.
     *
     * Example: "Holiday Notice", "Class Cancelled"
     */
    @Column(nullable = false)
    private String title;

    /**
     * Announcement ka message (mail ka body).
     *
     * Example: "Kal ki class 8 PM se shuru hogi."
     */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String message;

    /**
     * NAYA FIELD --- API 8 KI SABSE ZAROORI CHEEZ
     *
     * Announcement kahan tak jayega:
     *
     * SPECIFIC_BATCH  -> batchId diya gaya hai
     *                    sirf uss batch ke students ko mail
     *
     * ALL_MY_BATCHES  -> batchId nahi diya
     *                    Trainer ke SAARE batches ke students ko mail
     *
     * GLOBAL          -> Admin ne bina batchId ke bheja
     *                    Institute ke saare students ko mail
     *
     * Iss field se batch = null wale case bhi
     * clearly samajh aa jate hain.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AnnouncementScope scope;

    /**
     * Many-to-One
     *
     * Announcement kis Batch ka hai.
     *
     * IMPORTANT: ab ye NULLABLE hai.
     *
     * scope = SPECIFIC_BATCH  -> batch set hoga
     * scope = ALL_MY_BATCHES  -> null
     * scope = GLOBAL          -> null
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id")
    private Batch batch;

    /**
     * Many-to-One
     *
     * Kis Trainer ne bheja.
     * Admin ne bheja to null.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trainer_id")
    private Trainer trainer;

    /**
     * Many-to-One
     *
     * Kis Admin ne bheja.
     * Trainer ne bheja to null.
     *
     * NOTE: trainer aur admin dono me se
     * hamesha EXACTLY EK hi set hoga.
     * Ye check service layer me lagayenge.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id")
    private Admin admin;

    /**
     * NAYA FIELD
     *
     * Important announcement upar pin ho jaye.
     * Example: "Exam Schedule"
     */
    @Builder.Default
    @Column(nullable = false)
    private Boolean pinned = false;

    /**
     * NAYE FIELDS --- mail tracking
     *
     * mailSent      -> mail successfully gaya ya nahi
     * mailSentAt    -> kab gaya
     * recipientCount-> kitne logo ko gaya
     *
     * Ye isliye chahiye kyunki mail bhejna
     * async hoga. Agar SMTP fail ho jaye to
     * pata chalna chahiye ki kis announcement
     * ka mail pending reh gaya (retry kar sakein).
     */
    @Builder.Default
    @Column(nullable = false)
    private Boolean mailSent = false;

    private LocalDateTime mailSentAt;

    @Builder.Default
    private Integer recipientCount = 0;

    /**
     * NAYA FIELD
     *
     * Soft delete.
     */
    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;

    /**
     * One-to-Many
     *
     * Announcement ke saath attach kiye gaye contents.
     *
     * "Java Notes Uploaded"
     *      |---- PDF Link
     *      |---- YouTube Link
     *
     * cascade = ALL -> announcement delete pe
     *                  uske attached contents bhi delete
     */
    @Builder.Default
    @OneToMany(mappedBy = "announcement", cascade = CascadeType.ALL)
    private List<Content> contents = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}