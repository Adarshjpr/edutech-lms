package com.uncodemy.lms.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

/**
 * ============================================================================
 * StudentCreateRequest   ---  API 7 ka INPUT
 * ============================================================================
 *
 * POST /api/admin/students     (admin  -> seedha APPROVED)
 * POST /api/trainer/students   (trainer -> PENDING)
 *
 * Sample:
 * {
 *   "name"    : "Adarsh Jha",
 *   "email"   : "adarsh@gmail.com",
 *   "phone"   : "9876543210",
 *   "course"  : "Java Full Stack",
 *   "batchId" : "JAVA101"
 * }
 *
 * batchId OPTIONAL hai
 * ---------------------------------------------------------------------------
 * Diya  -> student banega AUR batch me enroll bhi ho jayega
 * Nahi  -> sirf student banega, baad me API 5 se batch me daal denge
 *
 * studentId yahan nahi hai — system banayega (STU101, STU102...)
 * ============================================================================
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentCreateRequest {

    @NotBlank(message = "Student ka naam zaroori hai")
    @Size(min = 2, max = 100, message = "Naam 2 se 100 character ka hona chahiye")
    private String name;

    /**
     * Email — announcement aur content ki mail isi pe jayegi.
     * Isliye unique aur sahi honi chahiye.
     */
    @NotBlank(message = "Email zaroori hai")
    @Email(message = "Email ka format sahi nahi hai")
    @Size(max = 150)
    private String email;

    @Pattern(regexp = "^[0-9]{10,15}$", message = "Phone me 10 se 15 digits hone chahiye")
    private String phone;

    /**
     * Kaunsa course le raha hai.
     * Example: "Java Full Stack", "MERN Stack"
     */
    @Size(max = 100, message = "Course name 100 character se lamba nahi ho sakta")
    private String course;

    /**
     * Batch me turant enroll karna hai to batchId do.
     *
     * Trainer ke case me: ye batch USI ka hona chahiye,
     * warna 400 aayega.
     */
    @Size(max = 20)
    private String batchId;
}