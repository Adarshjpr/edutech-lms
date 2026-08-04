package com.uncodemy.lms.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * ============================================================================
 * JpaAuditingConfig
 * ============================================================================
 *
 * Ye class Spring Data JPA ka AUDITING feature ON karti hai.
 *
 * Auditing kya hai?
 * ---------------------------------------------------------------------------
 * Entity me humne ye do fields likhi hain:
 *
 *     @CreatedDate
 *     private LocalDateTime createdAt;
 *
 *     @LastModifiedDate
 *     private LocalDateTime updatedAt;
 *
 * Ab agar humne DB me row save ki, to Spring khud-ba-khud
 * inme date-time bhar dega. Manually setCreatedAt(...) likhne
 * ki zarurat nahi padegi.
 *
 * DO CHEEZEIN ZAROORI HAIN (dono lagengi tabhi kaam karega)
 * ---------------------------------------------------------------------------
 *
 *   1. Entity par  ->  @EntityListeners(AuditingEntityListener.class)
 *                      (ye humne Phase 0 me har entity me laga diya hai)
 *
 *   2. Project me   ->  @EnableJpaAuditing
 *                      (ye wali cheez YAHIN ho rahi hai)
 *
 * Sirf ek lagane se kuch nahi hoga — dono chahiye.
 *
 * COMMON BUG
 * ---------------------------------------------------------------------------
 * "Maine @CreatedDate laga rakha hai phir bhi DB me NULL aa raha hai"
 *
 * -> 99% case me @EnableJpaAuditing missing hota hai.
 *    Isi problem ko ye file solve karti hai.
 *
 * NOTE
 * ---------------------------------------------------------------------------
 * Aage jab Security add karenge, to isi class me
 * @CreatedBy / @LastModifiedBy ke liye AuditorAware bean
 * bhi add kar denge (kis user ne change kiya wo bhi track hoga).
 * Abhi security nahi hai, isliye sirf date-time.
 * ============================================================================
 */
@Configuration          // Spring ko batata hai ki ye ek config class hai
@EnableJpaAuditing      // Auditing feature ko ON karta hai
public class JpaAuditingConfig {

    // Body khali hai — bas annotation ka kaam tha.
    // Aage AuditorAware bean yahan add hoga.
}