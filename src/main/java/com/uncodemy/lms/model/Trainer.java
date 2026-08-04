package com.uncodemy.lms.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.uncodemy.lms.model.enums.TrainerRole;

/**
 * Trainer Entity
 *
 * Ye class database ke "trainers" table ko represent karti hai.
 *
 * Example:
 * Trainer ID : TR101
 * Name       : Rahul Sharma
 * Username   : rahul.sharma
 * Role       : TRAINER
 */
@Entity
@Table(name = "trainers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)   // createdAt / updatedAt auto-fill
public class Trainer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Trainer ka unique ID (system generate karega).
     * Example: TR101, TR102
     */
    @Column(name = "trainer_id", unique = true, nullable = false)
    private String trainerId;

    /**
     * Trainer ka naam.
     * API 10 me isi name se search hoga.
     */
    @Column(nullable = false)
    private String name;

    /**
     * NAYA FIELD
     *
     * Login Username.
     *
     * API 2 ke hisaab se trainer ko mail me
     * USERNAME + PASSWORD dono jaayenge.
     *
     * System auto-generate karega, example:
     * "Rahul Sharma" -> rahul.sharma
     * agar already exist kare -> rahul.sharma1
     */
    @Column(unique = true, nullable = false)
    private String username;

    /**
     * Trainer ki Designation.
     * Example: Senior Java Trainer
     */
    private String designation;

    /**
     * NAYA FIELD
     *
     * Trainer ka mobile number.
     */
    @Column(length = 15)
    private String phone;

    /**
     * Trainer ka Role.
     * Example: TRAINER, PLACEMENT_TEAM
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TrainerRole role;

    /**
     * Login Email (unique).
     * Isi pe credentials mail jayegi.
     */
    @Column(unique = true, nullable = false)
    private String email;

    /**
     * Login Password (BCrypt hashed).
     *
     * Trainer create hote waqt system random password
     * generate karega, mail me plain bhejega,
     * database me sirf hash rakhega.
     */
    @Column(nullable = false)
    private String password;

    /**
     * NAYA FIELD
     *
     * Pehli login pe password change force karne ke liye.
     * Create hote waqt true, password change karte hi false.
     */
    @Builder.Default
    @Column(nullable = false)
    private Boolean firstLogin = true;

    /**
     * NAYA FIELD
     *
     * Trainer active hai ya nahi (soft delete).
     * Trainer delete karna risky hai kyunki uske
     * batches / contents sab uss se jude hote hain.
     */
    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;

    /**
     * One-to-Many
     *
     * Ek Trainer multiple batches handle kar sakta hai.
     *
     * Rahul Sir
     *    |------ Java Batch
     *    |------ Spring Boot Batch
     */
    @Builder.Default
    @OneToMany(mappedBy = "trainer")
    private List<Batch> batches = new ArrayList<>();

    /**
     * One-to-Many
     * Ek Trainer multiple announcements post kar sakta hai.
     */
    @Builder.Default
    @OneToMany(mappedBy = "trainer")
    private List<Announcement> announcements = new ArrayList<>();

    /**
     * One-to-Many
     * Ek Trainer multiple study contents upload kar sakta hai.
     */
    @Builder.Default
    @OneToMany(mappedBy = "trainer")
    private List<Content> contents = new ArrayList<>();

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}