package com.uncodemy.lms.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Admin Entity
 *
 * Ye class database ke "admins" table ko represent karti hai.
 *
 * Example:
 * Admin ID : ADM101
 * Name     : Adarsh
 * Email    : admin@gmail.com
 */
@Entity
@Table(name = "admins")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)   // NAYA: createdAt/updatedAt auto-fill ke liye zaroori
public class Admin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Admin ka unique ID.
     * Example: ADM101, ADM102
     */
    @Column(name = "admin_id", unique = true, nullable = false)
    private String adminId;

    /**
     * Admin ka naam.
     * Example: Adarsh
     */
    @Column(nullable = false)
    private String name;

    /**
     * Admin ki Email ID. Login ke kaam bhi aayegi.
     * Example: admin@gmail.com
     */
    @Column(unique = true, nullable = false)
    private String email;

    /**
     * NAYA FIELD
     *
     * Login Password (BCrypt hashed).
     *
     * Abhi security skip kar rahe hain, lekin column
     * ab hi bana lo taaki baad me migration na likhni pade.
     *
     * Plain text kabhi store nahi hoga.
     */
    @Column(nullable = false)
    private String password;

    /**
     * NAYA FIELD
     *
     * Admin ka mobile number.
     * Aage WhatsApp / SMS notification me kaam aayega.
     *
     * Example: 9876543210
     */
    @Column(length = 15)
    private String phone;

    /**
     * NAYA FIELD
     *
     * Admin active hai ya nahi.
     * Delete karne ke bajay isse false kar denge (soft delete).
     */
    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;

    /**
     * One-to-Many Relationship
     *
     * Ek Admin multiple announcements create kar sakta hai.
     *
     * Admin
     *   |------ Holiday Notice
     *   |------ Fee Reminder
     */
    @Builder.Default
    @OneToMany(mappedBy = "admin")
    private List<Announcement> announcements = new ArrayList<>();

    @CreatedDate
    @Column(updatable = false)              // create hone ke baad kabhi change nahi hoga
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}