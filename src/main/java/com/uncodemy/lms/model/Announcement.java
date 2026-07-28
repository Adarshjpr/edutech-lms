package com.uncodemy.lms.model;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
/**
 * Announcement Entity
 *
 * Ye class database ke "announcements" table ko represent karti hai.
 * Isme Batch, Trainer ya Admin ke dwara bheje gaye announcements store hote hain.
 *
 * Example:
 * ------------------------------------------
 * Message : Tomorrow class will start at 8 PM.
 * Batch   : JAVA101
 * Trainer : Rahul Sir
 * Admin   : Adarsh
 * ------------------------------------------
 */
@Entity                           // Is class ko Database Entity banata hai.
@Table(name = "announcements")    // Database me table ka naam "announcements" hoga.
@Getter                           // Automatically Getters generate karega.
@Setter                           // Automatically Setters generate karega.
@NoArgsConstructor                // Default Constructor banata hai.
@AllArgsConstructor               // Sabhi fields wala Constructor banata hai.
@Builder                          // Builder Pattern support karta hai.
public class Announcement {

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
     * Announcement ka message.
     *
     * Example:
     * "Today's class is cancelled."
     * "Assignment has been uploaded."
     * "Tomorrow is holiday."
     *
     * TEXT use kiya gaya hai kyunki
     * message kabhi-kabhi kaafi lamba ho sakta hai.
     */
    @Column(columnDefinition = "TEXT")
    private String message;

    /**
     * Many-to-One Relationship
     *
     * Ek Batch me bahut saare Announcements ho sakte hain.
     * Lekin ek Announcement sirf ek Batch ka hoga.
     *
     * Example:
     *
     * JAVA101 Batch
     *      |
     *      |------ Class Cancelled
     *      |------ Assignment Uploaded
     *      |------ Exam Notice
     */
    @ManyToOne
    @JoinColumn(name = "batch_id")   // announcements table me batch_id Foreign Key banegi.
    private Batch batch;

    /**
     * Many-to-One Relationship
     *
     * Ek Trainer multiple announcements bhej sakta hai.
     * Lekin ek Announcement sirf ek Trainer se related hoga.
     *
     * Example:
     *
     * Rahul Sir
     *     |
     *     |------ Spring Boot Class Today
     *     |------ Java Notes Uploaded
     */
    @ManyToOne
    @JoinColumn(name = "trainer_id") // announcements table me trainer_id Foreign Key banegi.
    private Trainer trainer;

    /**
     * Many-to-One Relationship
     *
     * Ek Admin bhi multiple announcements bhej sakta hai.
     * Lekin ek Announcement sirf ek Admin ka hoga.
     *
     * Example:
     *
     * Admin
     *    |
     *    |------ Holiday Notice
     *    |------ Fee Reminder
     *    |------ New Batch Started
     */
    @ManyToOne
    @JoinColumn(name = "admin_id")   // announcements table me admin_id Foreign Key banegi.
    private Admin admin;

    /**
     * Announcement kab create hua tha.
     *
     * Example:
     * 2026-07-28T10:30:15
     *
     * LocalDateTime Date aur Time dono ko store karta hai.
     */


    /**
     * One-to-Many Relationship
     *
     * Ek Announcement ke andar multiple Contents ho sakte hain.
     *
     * Example:
     *
     * Announcement:
     * "Java Notes Uploaded"
     *      |
     *      |------ PDF Link
     *      |------ YouTube Link
     *      |------ Source Code ZIP
     *
     * cascade = CascadeType.ALL
     * -> Agar Announcement Save/Delete hoga,
     *    to uske saare Contents par bhi wahi operation perform hoga.
     */
    @OneToMany(mappedBy = "announcement", cascade = CascadeType.ALL)
    private List<Content> contents = new ArrayList<>();

    /**
     * ArrayList kyu use ki gayi?
     *
     * ✔ Multiple Contents store karne ke liye.
     * ✔ Insertion order maintain rehta hai.
     * ✔ Dynamic size hoti hai.
     * ✔ Index ke through access kar sakte hain.
     */
    @CreatedDate
private LocalDateTime createdAt;

@LastModifiedDate
private LocalDateTime updatedAt;
}