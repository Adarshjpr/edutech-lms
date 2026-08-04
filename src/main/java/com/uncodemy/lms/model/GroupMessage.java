package com.uncodemy.lms.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.uncodemy.lms.model.enums.MessageType;
import com.uncodemy.lms.model.enums.SenderType;

/**
 * ============================================================================
 * GroupMessage Entity   (NAYI)  --- API 12
 * ============================================================================
 *
 * Har Batch ka apna ek group hota hai (WhatsApp jaisa).
 * Alag "Group" table banane ki zarurat nahi —
 * BATCH khud hi group hai.
 *
 * Group ka member kaun hai?
 * -> student_batch table jo bolti hai wahi (active = true).
 *    Alag membership table maintain karne ki zarurat nahi.
 *
 * 7 DAYS DISAPPEARING
 * ---------------------------------------------------------------------------
 * Message banate waqt hi expiresAt = createdAt + 7 din set ho jayega.
 *
 * Do level ka cleanup:
 *
 * 1. READ TIME  -> query me hamesha "expiresAt > now()" ka filter,
 *                  taaki expire hua message turant dikhna band ho jaye
 *                  (scheduler ka intezaar na karna pade)
 *
 * 2. SCHEDULER  -> roz raat ko purani rows DELETE + Cloudinary
 *                  se file bhi delete
 *
 * ---------------------------------------------------------------------------
 * SENDER kaun hai?
 *
 * Security abhi hai nahi, isliye teeno FK nullable rakhe hain
 * aur senderType se pata chalega ki kaunsa wala bhara hai.
 *
 * senderType = STUDENT -> student field set
 * senderType = TRAINER -> trainer field set
 * senderType = ADMIN   -> admin field set
 *
 * (Ek hi "senderId Long" rakh sakte the, lekin phir naam
 *  nikalne ke liye har baar manually query karni padti.
 *  FK rakhne se JOIN me hi naam aa jayega.)
 * ============================================================================
 */
@Entity
@Table(
    name = "group_messages",
    indexes = {
        // Chat kholte hi "iss batch ke latest messages" — sabse zyada chalne wali query
        @Index(name = "idx_gm_batch_created", columnList = "batch_id, created_at"),
        // Scheduler ke liye: "expire ho chuke messages dhundo"
        @Index(name = "idx_gm_expires", columnList = "expires_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class GroupMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Message kis Batch ke group me bheja gaya.
     *
     * Ye hi "group id" hai.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    private Batch batch;

    /**
     * Message ka type.
     *
     * TEXT  -> sirf message
     * PDF   -> file Cloudinary pe
     * IMAGE -> photo Cloudinary pe
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageType messageType;

    /**
     * Message ka text.
     *
     * TEXT type me -> actual message
     * PDF/IMAGE me -> caption (optional, null ho sakta hai)
     */
    @Column(columnDefinition = "TEXT")
    private String message;

    /**
     * Cloudinary pe upload hui file ka URL.
     * TEXT message me null.
     */
    @Column(columnDefinition = "TEXT")
    private String fileUrl;

    /**
     * Cloudinary ka public_id.
     *
     * 7 din baad scheduler isi se Cloudinary
     * se file delete karega. URL se delete nahi hota.
     */
    private String cloudinaryPublicId;

    /**
     * File ka original naam.
     * Example: "Java_Day12_Notes.pdf"
     *
     * Cloudinary apna random naam de deta hai,
     * isliye asli naam alag se store karna padta hai.
     */
    private String fileName;

    /**
     * File ka size bytes me.
     * UI pe "2.4 MB" dikhane ke liye.
     */
    private Long fileSize;

    // ---------------------- SENDER ----------------------

    /**
     * Bhejne wala kaun hai.
     * Iske hisaab se neeche wale 3 me se ek field bhara hoga.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "sender_type", nullable = false)
    private SenderType senderType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_student_id")
    private Student senderStudent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_trainer_id")
    private Trainer senderTrainer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_admin_id")
    private Admin senderAdmin;

    // ---------------------- DISAPPEARING ----------------------

    /**
     * Message kab gayab hoga.
     *
     * Service set karegi: createdAt + 7 din
     *
     * Read query me hamesha:
     *   WHERE expiresAt > CURRENT_TIMESTAMP
     */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /**
     * User ne khud message delete kiya ("delete for everyone").
     *
     * Row turant nahi hatayenge, kyunki uska
     * Cloudinary file bhi delete karna hota hai —
     * wo kaam scheduler karega.
     */
    @Builder.Default
    @Column(nullable = false)
    private Boolean deleted = false;

    /**
     * Ye message kis message ka reply hai (optional).
     *
     * Self-reference: GroupMessage -> GroupMessage
     *
     * WhatsApp me jaise upar quote dikhta hai, wahi.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reply_to_id")
    private GroupMessage replyTo;

    /**
     * NOTE: updatedAt nahi rakha.
     * Chat message edit nahi hota, sirf delete hota hai.
     */
    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;
}