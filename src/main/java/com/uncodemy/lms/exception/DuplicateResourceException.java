package com.uncodemy.lms.exception;

/**
 * ============================================================================
 * DuplicateResourceException   ->  HTTP 409 (CONFLICT)
 * ============================================================================
 *
 * Jab koi cheez pehle se exist karti ho.
 *
 * Iss project me jahan-jahan unique = true laga hai,
 * wahan ye exception aayegi:
 *
 *   Trainer  -> trainerId, username, email
 *   Student  -> studentId, email
 *   Batch    -> batchId
 *   Admin    -> adminId, email
 *
 * Example:
 *   if (trainerRepository.existsByEmail(request.getEmail())) {
 *       throw new DuplicateResourceException("Trainer", "email", request.getEmail());
 *   }
 *
 * Response:
 * {
 *   "success" : false,
 *   "message" : "Trainer already exists with email : 'rahul@gmail.com'"
 * }
 *
 * 400 ke bajay 409 kyun?
 * ---------------------------------------------------------------------------
 * 400 = "tumne galat data bheja"
 * 409 = "data to sahi hai, lekin ye cheez already hai"
 *
 * Frontend inhe alag-alag handle kar sakta hai —
 * 409 pe "Ye email already registered hai, login karo?" dikha sakta hai.
 * ============================================================================
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }

    /**
     * Structured constructor.
     *
     * Banega: Trainer already exists with email : 'rahul@gmail.com'
     */
    public DuplicateResourceException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s already exists with %s : '%s'", resourceName, fieldName, fieldValue));
    }
}