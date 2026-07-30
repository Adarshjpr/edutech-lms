package com.uncodemy.lms.model;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import com.uncodemy.lms.model.enums.BatchStatus;

/**
 * Batch Entity
 *
 * Ye class database ke "batches" table ko represent karti hai.
 * Isme har batch ki information store hoti hai.
 *
 * Example:
 * Batch ID   : JAVA-101
 * Batch Name : Java Full Stack
 * Timing     : 7:00 PM - 9:00 PM
 */
@Entity                        // Is class ko Database Entity banata hai.
@Table(name = "batches")         // Database me table ka naam "batches" hoga.
@Getter                          // Automatically Getters generate karega.
@Setter                          // Automatically Setters generate karega.
@NoArgsConstructor               // Default Constructor banata hai.
@AllArgsConstructor              // Sabhi fields wala Constructor banata hai.
@Builder                         // Builder Pattern support karta hai.
public class Batch {

    /**
     * Primary Key
     *
     * Database khud unique ID generate karega.
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
     * Batch ka unique ID.
     *
     * Example:
     * JAVA101
     * MERN201
     *
     * unique = true
     * -> Same Batch ID do baar nahi ho sakti.
     */
    @Column(name = "batch_id", unique = true)
    private String batchId;

    /**
     * Batch ka naam.
     *
     * Example:
     * Java Full Stack
     * MERN Stack
     * Python
     */
    private String batchName;

    /**
     * Batch ka timing.
     *
     * Example:
     * 10 AM - 12 PM
     * 7 PM - 9 PM
     */
    private String timing;

    /**
     * Online Class Meeting Link.
     *
     * Example:
     * Google Meet
     * Zoom
     *
     * columnDefinition = "TEXT"
     * Kyunki URL kabhi-kabhi kaafi lamba ho sakta hai.
     */
    @Column(columnDefinition = "TEXT")
    private String meetLink;

    /**
     * Certificate Download Link.
     *
     * Example:
     * https://certificate.abc.com/123
     *
     * TEXT use kiya gaya hai taaki long URL store ho sake.
     */
    @Column(columnDefinition = "TEXT")
    private String certificateLink;

    /**
     * Batch me currently kya topic chal raha hai.
     *
     * Example:
     * Spring Boot
     * React Hooks
     * Java Collection Framework
     */
    private String currentTopic;

    /**
     * Batch ka current status.
     *
     * Example:
     * ACTIVE
     * COMPLETED
     * UPCOMING
     *
     * Database me String ke form me save hoga.
     */
    @Enumerated(EnumType.STRING)
    private BatchStatus status;

    /**
     * Many-to-One Relationship
     *
     * Ek Trainer multiple batches padha sakta hai.
     * Lekin ek Batch ka sirf ek Trainer hoga.
     *
     * Example:
     *
     * Trainer Rahul Sir
     *      |
     *      |------ Java Batch
     *      |------ Spring Batch
     *      |------ DSA Batch
     */
    @ManyToOne
    @JoinColumn(name = "trainer_id")   // batches table me trainer_id Foreign Key banegi.
    private Trainer trainer;

   /**
 * StudentBatch Entity
 *
 * Ye Entity Student aur Batch ke beech ke relationship
 * ko represent karti hai.
 *
 * Is Entity ko Enrollment Table bhi kaha ja sakta hai.
 *
 * Direct Many-to-Many relation use nahi kiya gaya,
 * kyunki future me enrollment se related extra
 * information store karni pad sakti hai.
 *
 * Current Fields:
 * -----------------------------
 * id
 * student_id (FK)
 * batch_id (FK)
 * joined_at
 *
 * Database Structure:
 *
 * student_batch
 * ----------------------------------------
 * id | student_id | batch_id | joined_at
 * ----------------------------------------
 * 1  | STU101     | JAVA101  | 2026-07-28
 * 2  | STU101     | MERN201  | 2026-08-10
 * 3  | STU102     | JAVA101  | 2026-07-30
 *
 * Benefits:
 * -----------------------------
 * ✔ Production Ready
 * ✔ Scalable Design
 * ✔ Extra fields easily add kiye ja sakte hain
 * ✔ Student aur Batch ka relation clean rehta hai
 */


    /**
     * One-to-Many Relationship
     *
     * Ek Batch me multiple Announcements ho sakte hain.
     * Lekin ek Announcement sirf ek Batch ka hoga.
     *
     * Example:
     *
     * Java Batch
     *     |
     *     |---- Class Cancel
     *     |---- Assignment Uploaded
     *     |---- Exam Date Announced
     */
    @OneToMany(mappedBy = "batch", cascade = CascadeType.ALL)
    private List<Announcement> announcements = new ArrayList<>();

    /**
     * HashSet
     * --------
     * Students ke liye HashSet use kiya gaya hai.
     *
     * Benefits:
     * ✔ Duplicate Student add nahi hoga.
     * ✔ Searching fast hoti hai.
     *
     *
     * ArrayList
     * ----------
     * Announcements ke liye ArrayList use ki gayi hai.
     *
     * Benefits:
     * ✔ Order maintain rehta hai.
     * ✔ Same type ke multiple announcements rakh sakte hain.
     * ✔ Index ke through access kar sakte hain.
     */
@Column(columnDefinition = "TEXT")
private String communityLink;
    @CreatedDate
private LocalDateTime createdAt;

@LastModifiedDate
private LocalDateTime updatedAt;
}