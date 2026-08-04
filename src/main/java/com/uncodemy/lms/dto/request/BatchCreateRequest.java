package com.uncodemy.lms.dto.request;

import com.uncodemy.lms.model.enums.BatchStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.*;

import java.time.LocalDate;

/**
 * ============================================================================
 * BatchCreateRequest   ---  API 3 aur API 4 ka INPUT
 * ============================================================================
 *
 * Dono jagah YAHI DTO use hoga:
 *
 *   POST /api/admin/batches      (Admin banaye)   -> seedha APPROVED
 *   POST /api/trainer/batches    (Trainer banaye) -> PENDING
 *
 * Farak sirf SERVICE me hai, input same hai.
 *
 * Sample JSON:
 * ---------------------------------------------------------------------------
 * {
 *   "batchName"     : "Java Full Stack",
 *   "timing"        : "7 PM - 9 PM",
 *   "trainerId"     : "TR101",
 *   "meetLink"      : "https://meet.google.com/abc-defg-hij",
 *   "currentTopic"  : "Spring Boot Basics",
 *   "startDate"     : "2026-08-05",
 *   "status"        : "UPCOMING"
 * }
 *
 * batchId YAHAN NAHI HAI
 * ---------------------------------------------------------------------------
 * Wo system khud banayega batchName se:
 *   "Java Full Stack" -> JAVA101
 *
 * Waise hi approvalStatus bhi nahi hai — wo service
 * decide karegi ki admin ne banaya ya trainer ne.
 * ============================================================================
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatchCreateRequest {

    /**
     * Batch ka naam. Example: "Java Full Stack"
     *
     * IMPORTANT : batchId isi se banti hai.
     *   "Java Full Stack" -> pehla word "Java" -> JAVA101
     */
    @NotBlank(message = "Batch ka naam zaroori hai")
    @Size(min = 3, max = 100, message = "Batch name 3 se 100 character ka hona chahiye")
    private String batchName;

    /**
     * Class ka time. Example: "7 PM - 9 PM"
     *
     * String rakha hai, LocalTime nahi — kyunki log
     * "Mon-Fri 7-9 PM" jaisa kuch bhi likhna chahte hain.
     */
    @Size(max = 50, message = "Timing 50 character se lambi nahi ho sakti")
    private String timing;

    /**
     * Kaunsa trainer padhayega.
     *
     * ADMIN ke case me : ye zaroori hai (kisi ko assign karna hai)
     * TRAINER ke case me : ignore hoga — trainer khud hi assign hoga
     *
     * Isliye yahan @NotBlank nahi lagaya. Admin wali service
     * khud check karegi ki null to nahi.
     */
    @Size(max = 20)
    private String trainerId;

    /**
     * Online class ka link (Google Meet / Zoom).
     */
    @Size(max = 500, message = "Meet link bahut lamba hai")
    private String meetLink;

    /**
     * WhatsApp / community group ka link.
     *
     * NOTE : API 12 wala in-app group iss se ALAG hai.
     * Ye sirf external link store karne ke liye hai.
     */
    @Size(max = 500)
    private String communityLink;

    /**
     * Certificate download ka link.
     */
    @Size(max = 500)
    private String certificateLink;

    /**
     * Batch me abhi kaunsa topic chal raha hai.
     *
     * Optional hai — batch banate waqt pata na ho to baad me
     * API 6 se set kar sakte hain.
     */
    @Size(max = 200, message = "Topic 200 character se lamba nahi ho sakta")
    private String currentTopic;

    /** Batch kab shuru hoga */
    private LocalDate startDate;

    /** Batch kab khatam hoga */
    private LocalDate endDate;

    /**
     * Batch ka status : UPCOMING / ACTIVE / COMPLETED
     *
     * Nahi bheja to service UPCOMING maan legi.
     */
    private BatchStatus status;
}