package com.uncodemy.lms.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.uncodemy.lms.model.enums.ContentType;

/**
 * Content Entity
 *
 * Trainer ke dwara upload kiye gaye study materials.
 *
 * Example:
 * ------------------------------------------
 * Title  : Spring Boot Notes
 * Type   : PDF
 * Link   : https://res.cloudinary.com/.....
 * Batch  : JAVA101
 * Date   : 2026-07-31
 * ------------------------------------------
 */
@Entity
@Table(
    name = "contents",
    indexes = {
        // API 9: batch ke hisaab se date-wise fetch fast ho
        @Index(name = "idx_content_batch_uploaded", columnList = "batch_id, uploaded_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Content {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Content ka title.
     * Example: Java Notes, React Recording
     */
    @Column(nullable = false)
    private String title;

    /**
     * NAYA FIELD
     *
     * Content ke baare me chhota sa description.
     * Optional hai.
     *
     * Example: "Day 12 - Collections ka complete notes"
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Content ka type.
     * Example: PDF, VIDEO, LINK, DOCUMENT
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContentType type;

    /**
     * Content ka URL / Download Link.
     *
     * Do tarah se aa sakta hai:
     * 1. Trainer ne seedha link paste kiya (YouTube, Drive)
     * 2. Trainer ne file upload ki -> Cloudinary ka URL
     */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String link;

    /**
     * NAYA FIELD
     *
     * Cloudinary ka public_id.
     *
     * Ye kyun chahiye?
     * Cloudinary se file DELETE karne ke liye URL kaam nahi aata,
     * public_id chahiye hota hai.
     *
     * Example: lms/contents/java_notes_a8f3k2
     *
     * Agar trainer ne sirf link paste kiya (upload nahi kiya)
     * to ye null rahega.
     */
    private String cloudinaryPublicId;

    /**
     * NAYA FIELD
     *
     * File ka size bytes me (sirf uploaded files ke liye).
     * UI pe "2.4 MB" dikhane ke kaam aayega.
     */
    private Long fileSize;

    /**
     * NAYA RELATION --- API 9 KI SABSE ZAROORI CHEEZ
     *
     * Content kis Batch ka hai.
     *
     * Pehle ye field thi hi nahi, isliye
     * "batch ke hisaab se content nikalo" possible nahi tha.
     *
     * JAVA101 Batch
     *    |---- Day 1 Notes
     *    |---- Day 2 Recording
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    private Batch batch;

    /**
     * Many-to-One
     *
     * Content kis Announcement ke saath attach hai.
     *
     * IMPORTANT: ab ye OPTIONAL (nullable) hai.
     *
     * Do case:
     * 1. Trainer ne sirf content upload kiya  -> announcement = null
     * 2. Announcement ke saath file bheji     -> announcement set
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "announcement_id")
    private Announcement announcement;

    /**
     * Many-to-One
     *
     * Kis Trainer ne upload kiya.
     * Admin ne upload kiya to null rahega.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trainer_id")
    private Trainer trainer;

    /**
     * NAYA FIELD
     *
     * Agar Admin ne content upload kiya ho.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by_admin_id")
    private Admin uploadedByAdmin;

    /**
     * API 9 --- "date wise save ho jaiye"
     *
     * Content kab upload hua.
     *
     * createdAt se alag kyun?
     * Kyunki trainer purani class ka content
     * baad me bhi upload kar sakta hai aur
     * manually date set kar sakta hai.
     *
     * Example: 2026-07-28T11:45:20
     */
    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;

    /**
     * NAYA FIELD
     *
     * Soft delete.
     * Content delete karne pe Cloudinary se file
     * turant nahi hatayenge, sirf ye false kar denge.
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