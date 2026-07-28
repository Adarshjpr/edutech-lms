package com.uncodemy.lms.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import com.uncodemy.lms.model.enums.TrainerRole;

/**
 * Trainer Entity
 *
 * Ye class database ke "trainers" table ko represent karti hai.
 * Isme trainer ki basic information store hoti hai.
 *
 * Example:
 * Trainer ID : TR101
 * Name       : Rahul Sharma
 * Role       : TRAINER
 * Designation: Senior Java Trainer
 */
@Entity                          // Is class ko Database Entity banata hai.
@Table(name = "trainers")        // Database me table ka naam "trainers" hoga.
@Getter                          // Automatically Getters generate karega.
@Setter                          // Automatically Setters generate karega.
@NoArgsConstructor               // Default Constructor banata hai.
@AllArgsConstructor              // Sabhi fields wala Constructor banata hai.
@Builder                         // Builder Pattern support karta hai.
public class Trainer {

    /**
     * Primary Key
     *
     * Database automatically unique ID generate karega.
     *
     * Example:
     * 1
     * 2
     * 3
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Trainer ka unique ID.
     *
     * Example:
     * TR101
     * TR102
     *
     * unique = true
     * -> Same Trainer ID do baar nahi ho sakti.
     */
    @Column(name = "trainer_id", unique = true)
    private String trainerId;

    /**
     * Trainer ka naam.
     *
     * Example:
     * Rahul Sharma
     * Amit Kumar
     */
    private String name;

    /**
     * Trainer ki Designation.
     *
     * Example:
     * Senior Java Trainer
     * MERN Trainer
     * Placement Mentor
     */
    private String designation;

    /**
     * Trainer ka Role.
     *
     * Enum use kiya gaya hai.
     *
     * Example:
     * TRAINER
     * ADMIN
     * PLACEMENT_TEAM
     *
     * Database me String ke form me save hoga.
     */
    @Enumerated(EnumType.STRING)
    private TrainerRole role;

    /**
     * One-to-Many Relationship
     *
     * Ek Trainer multiple batches handle kar sakta hai.
     * Lekin ek Batch ka sirf ek Trainer hoga.
     *
     * Example:
     *
     * Rahul Sir
     *    |
     *    |------ Java Batch
     *    |------ Spring Boot Batch
     *    |------ DSA Batch
     *
     * mappedBy = "trainer"
     * -> Relationship Batch Entity ke trainer field se manage ho raha hai.
     */
    @OneToMany(mappedBy = "trainer")
    private List<Batch> batches = new ArrayList<>();

    /**
     * One-to-Many Relationship
     *
     * Ek Trainer multiple announcements post kar sakta hai.
     * Lekin ek Announcement sirf ek Trainer ka hoga.
     *
     * Example:
     *
     * Rahul Sir
     *    |
     *    |------ Today's Class Cancelled
     *    |------ Assignment Uploaded
     *    |------ Exam Schedule
     *
     * mappedBy = "trainer"
     * -> Relationship Announcement Entity ke trainer field se manage hota hai.
     */
    @OneToMany(mappedBy = "trainer")
    private List<Announcement> announcements = new ArrayList<>();

    /**
     * One-to-Many Relationship
     *
     * Ek Trainer multiple study contents upload kar sakta hai.
     * Lekin ek Content sirf ek Trainer ka hoga.
     *
     * Example:
     *
     * Rahul Sir
     *    |
     *    |------ Java Notes
     *    |------ Spring Boot PDF
     *    |------ React Recording
     *
     * mappedBy = "trainer"
     * -> Relationship Content Entity ke trainer field se manage hota hai.
     */
    @OneToMany(mappedBy = "trainer")
    private List<Content> contents = new ArrayList<>();

    /**
     * ArrayList kyu use ki gayi?
     *
     * ✔ Multiple records store karne ke liye.
     * ✔ Insertion order maintain rehta hai.
     * ✔ Index ke through access kar sakte hain.
     * ✔ Dynamic size hoti hai (automatically badh jaati hai).
     */

    @CreatedDate
private LocalDateTime createdAt;

@LastModifiedDate
private LocalDateTime updatedAt;
}