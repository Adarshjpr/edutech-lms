package com.uncodemy.lms.model;



import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import com.uncodemy.lms.model.enums.StudentStatus;

/**
 * Student Entity
 *
 * Ye class database ke "students" table ko represent karti hai.
 * Isme har student ki basic information store hoti hai.
 */
@Entity                     // Batata hai ki ye ek JPA Entity hai.
@Table(name = "students")   // Database me table ka naam "students" hoga.
@Getter                     // Lombok automatically saare getters bana deta hai.
@Setter                     // Lombok automatically saare setters bana deta hai.
@NoArgsConstructor          // Default constructor create karta hai.
@AllArgsConstructor         // Sabhi fields wala constructor create karta hai.
@Builder                    // Object ko Builder Pattern se create karne ke liye.
public class Student {

    /**
     * Primary Key
     * Database har student ko ek unique ID degi.
     * Ye auto increment hogi.
     *
     * Example:
     * 1, 2, 3, 4...
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Student ka unique ID.
     *
     * Example:
     * STU101
     * STU102
     *
     * unique = true
     * -> Ek hi Student ID do baar nahi ho sakti.
     *
     * nullable = false
     * -> Student ID dena mandatory hai.
     */
    @Column(name = "student_id", unique = true, nullable = false)
    private String studentId;

    /**
     * Student ka naam.
     *
     * Example:
     * Adarsh Jha
     */
    private String name;

    /**
     * Student ki Email ID.
     *
     * unique = true
     * -> Same email se do students register nahi ho sakte.
     *
     * nullable = false
     * -> Email dena compulsory hai.
     */
    @Column(unique = true, nullable = false)
    private String email;
  
     private String Course;  // course bhi student ki aaye gi 
     
    /**
     * Student ka current status.
     *
     * Enum use kiya gaya hai.
     *
     * Example:
     * ACTIVE
     * INACTIVE
     * BLOCKED
     *
     * EnumType.STRING ka matlab database me
     * "ACTIVE" jaisa text store hoga,
     * number (0,1,2) nahi.
     */
    @Enumerated(EnumType.STRING)
    private StudentStatus status;

/**
 * One-to-Many Relationship
 *
 * Student aur Batch ke beech direct Many-to-Many relation
 * use nahi kiya gaya hai.
 *
 * Humne ek alag Entity "StudentBatch" banayi hai jo
 * Student ki enrollment information ko represent karti hai.
 *
 * Ek Student multiple batches join kar sakta hai,
 * isliye Student ke paas multiple StudentBatch records honge.
 *
 * Example:
 *
 * Student : Adarsh
 *
 * StudentBatch
 * --------------------------
 * JAVA101
 * MERN201
 * SPRING301
 *
 * Har StudentBatch record ke andar:
 * - Student
 * - Batch
 * - Joined Date
 *
 * Future me isi entity me aur fields bhi add ki ja sakti hain
 * jaise:
 * - Certificate Status
 * - Progress
 * - Attendance
 * - Payment Details
 *
 * CascadeType.ALL
 * ----------------
 * Student save/delete hone par related StudentBatch
 * records bhi automatically save/delete ho jayenge.
 *
 * orphanRemoval = true
 * --------------------
 * Agar Student se koi StudentBatch remove kar diya jaye,
 * to database se bhi automatically delete ho jayega.
 */
@OneToMany(mappedBy = "student",
        cascade = CascadeType.ALL,
        orphanRemoval = true)
private Set<StudentBatch> studentBatches = new HashSet<>();

@CreatedDate
private LocalDateTime createdAt;

@LastModifiedDate
private LocalDateTime updatedAt;
}