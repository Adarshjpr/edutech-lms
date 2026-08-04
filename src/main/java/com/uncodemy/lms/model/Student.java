package com.uncodemy.lms.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.uncodemy.lms.model.enums.StudentStatus;
import com.uncodemy.lms.model.enums.ApprovalStatus;

/**
 * Student Entity
 *
 * Ye class database ke "students" table ko represent karti hai.
 *
 * Example:
 * Student ID : STU101
 * Name       : Adarsh Jha
 * Course     : Java Full Stack
 */
@Entity
@Table(name = "students")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Student ka unique ID.
     * Example: STU101, STU102
     *
     * API 12 (Group Chat) me BATCH_ID + STUDENT_ID
     * ka combination hi group membership decide karega.
     */
    @Column(name = "student_id", unique = true, nullable = false)
    private String studentId;

    /**
     * Student ka naam.
     * Example: Adarsh Jha
     */
    @Column(nullable = false)
    private String name;

    /**
     * Student ki Email ID.
     * Announcement / Content ka mail isi pe jayega.
     */
    @Column(unique = true, nullable = false)
    private String email;

    /**
     * NAYA FIELD
     *
     * Student ka mobile number.
     * Group chat aur contact ke liye.
     */
    @Column(length = 15)
    private String phone;

    /**
     * BADLA HUA FIELD
     *
     * Pehle "Course" tha (capital C) -> ab "course".
     *
     * Java me field ka naam hamesha
     * chhote akshar se shuru hota hai (camelCase),
     * warna Lombok getter "getCourse()" nahi
     * dhang se banata aur JSON me "course" galat aata hai.
     *
     * Example: Java Full Stack, MERN Stack
     */
    private String course;

    /**
     * NAYA FIELD
     *
     * Login Password (BCrypt hashed).
     * Student portal + group chat ke liye.
     */
    private String password;

    /**
     * Student ka current status.
     * Example: ACTIVE, INACTIVE, BLOCKED
     */
    @Enumerated(EnumType.STRING)
    private StudentStatus status;

    /**
     * NAYA FIELD  --- API 7 ki core requirement
     *
     * Student kis stage pe hai:
     *
     * Admin ne add kiya    -> APPROVED  (seedha active)
     * Trainer ne add kiya  -> PENDING   (admin approve karega)
     *
     * PENDING student ko koi mail nahi jayega
     * aur na hi wo group chat me dikhega.
     */
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private ApprovalStatus approvalStatus = ApprovalStatus.APPROVED;

    /**
     * NAYA FIELD
     *
     * Kis Trainer ne is student ko add kiya tha.
     *
     * Agar Admin ne add kiya to ye null rahega.
     * Audit ke liye rakha hai — pata chale request
     * kisne bheji thi.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "added_by_trainer_id")
    private Trainer addedByTrainer;

    /**
     * One-to-Many Relationship
     *
     * Student aur Batch ke beech direct @ManyToMany
     * use nahi kiya gaya.
     *
     * Alag Entity "StudentBatch" (Enrollment) banayi hai.
     *
     * Student : Adarsh
     *    |---- JAVA101
     *    |---- MERN201
     *
     * CascadeType.ALL   -> Student save/delete pe enrollment bhi
     * orphanRemoval     -> list se hataya to DB se bhi delete
     */
    @Builder.Default
    @OneToMany(mappedBy = "student",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    private Set<StudentBatch> studentBatches = new HashSet<>();

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}