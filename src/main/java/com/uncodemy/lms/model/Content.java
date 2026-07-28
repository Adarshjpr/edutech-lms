package com.uncodemy.lms.model;



import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import com.uncodemy.lms.model.enums.ContentType;

/**
 * Content Entity
 *
 * Ye class database ke "contents" table ko represent karti hai.
 * Isme Trainer ke dwara upload kiye gaye study materials store hote hain.
 *
 * Example:
 * ------------------------------------------
 * Title : Spring Boot Notes
 * Type  : PDF
 * Link  : https://drive.google.com/.....
 * ------------------------------------------
 */
@Entity                           // Is class ko Database Entity banata hai.
@Table(name = "contents")         // Database me table ka naam "contents" hoga.
@Getter                           // Automatically Getters generate karega.
@Setter                           // Automatically Setters generate karega.
@NoArgsConstructor                // Default Constructor banata hai.
@AllArgsConstructor               // Sabhi fields wala Constructor banata hai.
@Builder                          // Builder Pattern support karta hai.
public class Content {

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
     * Content ka title.
     *
     * Example:
     * Java Notes
     * Spring Boot PPT
     * React Recording
     */
    private String title;

    /**
     * Content ka type.
     *
     * Enum use kiya gaya hai.
     *
     * Example:
     * PDF
     * VIDEO
     * LINK
     * DOCUMENT
     *
     * Database me String ke form me store hoga.
     */
    @Enumerated(EnumType.STRING)
    private ContentType type;

    /**
     * Content ka URL ya Download Link.
     *
     * Example:
     * https://drive.google.com/...
     * https://youtube.com/...
     *
     * TEXT use kiya gaya hai kyunki URL lamba ho sakta hai.
     */
    @Column(columnDefinition = "TEXT")
    private String link;

    /**
     * Many-to-One Relationship
     *
     * Ek Announcement ke andar multiple Contents ho sakte hain.
     * Lekin ek Content sirf ek Announcement se related hoga.
     *
     * Example:
     *
     * Announcement:
     * "Spring Boot Resources"
     *      |
     *      |------ PDF
     *      |------ Video
     *      |------ GitHub Link
     */
    @ManyToOne
    @JoinColumn(name = "announcement_id") // contents table me announcement_id Foreign Key banegi.
    private Announcement announcement;

    /**
     * Many-to-One Relationship
     *
     * Ek Trainer multiple Contents upload kar sakta hai.
     * Lekin ek Content sirf ek Trainer ne upload kiya hoga.
     *
     * Example:
     *
     * Rahul Sir
     *     |
     *     |------ Java Notes
     *     |------ Spring Boot PDF
     *     |------ React Recording
     */
    @ManyToOne
    @JoinColumn(name = "trainer_id") // contents table me trainer_id Foreign Key banegi.
    private Trainer trainer;

    /**
     * Content kab upload hua tha.
     *
     * Example:
     * 2026-07-28T11:45:20
     *
     * LocalDateTime Date aur Time dono ko store karta hai.
     */
    private LocalDateTime uploadedAt;
    @CreatedDate
private LocalDateTime createdAt;

@LastModifiedDate
private LocalDateTime updatedAt;
}