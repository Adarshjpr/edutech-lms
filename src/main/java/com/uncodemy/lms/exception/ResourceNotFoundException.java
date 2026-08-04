package com.uncodemy.lms.exception;

/**
 * ============================================================================
 * ResourceNotFoundException   ->  HTTP 404
 * ============================================================================
 *
 * Jab database me koi cheez mile hi na, tab ye throw hoti hai.
 *
 * Example:
 *   trainerRepository.findByTrainerId("TR999")
 *        .orElseThrow(() -> new ResourceNotFoundException("Trainer", "trainerId", "TR999"));
 *
 * Response banega:
 * {
 *   "success" : false,
 *   "message" : "Trainer not found with trainerId : 'TR999'"
 * }
 *
 * RuntimeException se extend kyun kiya?
 * ---------------------------------------------------------------------------
 * RuntimeException = "unchecked exception".
 * Matlab method me "throws" likhne ki zarurat nahi padti,
 * aur na hi har jagah try-catch lagana padta.
 *
 * Sidha GlobalExceptionHandler tak pahunch jayegi.
 * ============================================================================
 */
public class ResourceNotFoundException extends RuntimeException {

    /**
     * Simple message wala constructor.
     *
     * Example:
     * throw new ResourceNotFoundException("Koi bhi batch nahi mila");
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }

    /**
     * Structured constructor — message khud ban jayega.
     *
     * resourceName -> "Trainer"
     * fieldName    -> "trainerId"
     * fieldValue   -> "TR999"
     *
     * Banega: Trainer not found with trainerId : 'TR999'
     *
     * Fayda: pura project me error message ka format ek jaisa rahega.
     */
    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s not found with %s : '%s'", resourceName, fieldName, fieldValue));
    }
}