package com.uncodemy.lms.exception;

/**
 * ============================================================================
 * BadRequestException   ->  HTTP 400
 * ============================================================================
 *
 * Jab user ne galat cheez bheji ho, ya business rule toot raha ho.
 *
 * Iss project me ye jagah-jagah lagegi:
 *
 *   API 4  -> "Ye batch pehle se APPROVED hai, dobara approve nahi hoga"
 *   API 7  -> "Student is batch me pehle se enrolled hai"
 *   API 8  -> "Ye batch aapka nahi hai, announcement nahi bhej sakte"
 *   API 8  -> "PENDING batch me announcement nahi ja sakta"
 *   API 9  -> "File 10MB se badi nahi ho sakti"
 *
 * NOTE:
 * ---------------------------------------------------------------------------
 * @Valid wali field validation (jaise "email khali hai")
 * apne aap handle ho jayegi — uske liye ye exception nahi
 * lagani padegi. Ye sirf BUSINESS LOGIC ki galtiyon ke liye hai.
 * ============================================================================
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}