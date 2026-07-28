package com.uncodemy.lms.model;



import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

/**
 * Admin Entity
 *
 * Ye class database ke "admins" table ko represent karti hai.
 * Isme Admin ki basic information store hoti hai.
 *
 * Example:
 * Admin ID : ADM101
 * Name     : Adarsh
 * Email    : admin@gmail.com
 */
@Entity                          // Is class ko Database Entity banata hai.
@Table(name = "admins")          // Database me table ka naam "admins" hoga.
@Getter                          // Automatically Getters generate karega.
@Setter                          // Automatically Setters generate karega.
@NoArgsConstructor               // Default Constructor banata hai.
@AllArgsConstructor              // Sabhi fields wala Constructor banata hai.
@Builder                         // Builder Pattern support karta hai.
public class Admin {

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
     * Admin ka unique ID.
     *
     * Example:
     * ADM101
     * ADM102
     *
     * unique = true
     * -> Same Admin ID do baar nahi ho sakti.
     */
    @Column(name = "admin_id", unique = true)
    private String adminId;

    /**
     * Admin ka naam.
     *
     * Example:
     * Adarsh
     * Rahul Sharma
     */
    private String name;

    /**
     * Admin ki Email ID.
     *
     * Example:
     * admin@gmail.com
     *
     * unique = true
     * -> Ek email se sirf ek Admin register hoga.
     */
    @Column(unique = true)
    private String email;

    /**
     * One-to-Many Relationship
     *
     * Ek Admin multiple announcements create kar sakta hai.
     * Lekin ek Announcement sirf ek Admin ka hoga.
     *
     * Example:
     *
     * Admin
     *   |
     *   |------ Holiday Notice
     *   |------ New Batch Started
     *   |------ Exam Schedule
     *   |------ Fee Reminder
     *
     * mappedBy = "admin"
     * -> Relationship Announcement Entity ke
     *    "admin" field se manage ho raha hai.
     */
    @OneToMany(mappedBy = "admin")
    private List<Announcement> announcements = new ArrayList<>();

    /**
     * ArrayList kyu use ki gayi?
     *
     * ✔ Multiple Announcements store karne ke liye.
     * ✔ Order maintain rehta hai.
     * ✔ Index ke through access kar sakte hain.
     * ✔ Dynamic size hoti hai.
     * 
     */

    @CreatedDate
private LocalDateTime createdAt;

@LastModifiedDate
private LocalDateTime updatedAt;
}