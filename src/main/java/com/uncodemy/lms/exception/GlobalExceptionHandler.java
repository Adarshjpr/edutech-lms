package com.uncodemy.lms.exception;

import com.uncodemy.lms.dto.ApiResponse;

import jakarta.servlet.http.HttpServletRequest;

import lombok.extern.slf4j.Slf4j;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.HashMap;
import java.util.Map;

/**
 * ============================================================================
 * GlobalExceptionHandler
 * ============================================================================
 *
 * Pure project ki SAARI exceptions yahan aa kar handle hoti hain.
 *
 * @RestControllerAdvice
 * ---------------------------------------------------------------------------
 * Ye annotation Spring ko bolta hai:
 * "Kisi bhi controller me exception aaye, pehle iss class ko dikhana."
 *
 * ISKA FAYDA
 * ---------------------------------------------------------------------------
 * Controller me try-catch likhna hi nahi padta.
 *
 * GALAT tareeka (aisa MAT karna):
 *
 *     try {
 *         trainerService.create(req);
 *     } catch (Exception e) {
 *         return ResponseEntity.badRequest().body(e.getMessage());
 *     }
 *
 * SAHI tareeka:
 *
 *     TrainerResponse res = trainerService.create(req);   // bas itna
 *     return ResponseEntity.ok(ApiResponse.success("Created", res));
 *
 * Exception aayi to Spring khud yahan bhej dega.
 *
 * ORDER
 * ---------------------------------------------------------------------------
 * Spring hamesha SABSE SPECIFIC handler pehle dhundta hai.
 * Isliye Exception.class wala handler sabse neeche rakha hai —
 * wo sirf tab chalega jab upar ka koi match na kare.
 * ============================================================================
 */
@Slf4j                      // Lombok: "log" naam ka logger bana deta hai
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ========================================================================
    // 1. RESOURCE NOT FOUND  ->  404
    // ========================================================================
    /**
     * Jab DB me cheez mile hi na.
     *
     * Example: "Trainer not found with trainerId : 'TR999'"
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {

        log.warn("404 | {} | {}", request.getRequestURI(), ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }


    // ========================================================================
    // 2. BAD REQUEST (business rule toota)  ->  400
    // ========================================================================
    /**
     * Example: "Ye batch pehle se APPROVED hai"
     */
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(
            BadRequestException ex, HttpServletRequest request) {

        log.warn("400 | {} | {}", request.getRequestURI(), ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }


    // ========================================================================
    // 3. DUPLICATE  ->  409 CONFLICT
    // ========================================================================
    /**
     * Example: "Trainer already exists with email : 'rahul@gmail.com'"
     */
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicate(
            DuplicateResourceException ex, HttpServletRequest request) {

        log.warn("409 | {} | {}", request.getRequestURI(), ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage()));
    }


    // ========================================================================
    // 4. VALIDATION FAIL (@Valid)  ->  400
    // ========================================================================
    /**
     * Jab DTO me lagi validation fail ho.
     *
     * DTO me:
     *     @NotBlank(message = "Name is required")
     *     private String name;
     *
     * User ne name khali bheja -> ye handler chalega.
     *
     * Response:
     * {
     *   "success" : false,
     *   "message" : "Validation failed",
     *   "errors"  : {
     *       "name"  : "Name is required",
     *       "email" : "Email format galat hai"
     *   }
     * }
     *
     * Frontend har field ke neeche seedha error dikha sakta hai.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        Map<String, String> fieldErrors = new HashMap<>();

        // Har failed field ka naam + message nikal rahe hain
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }

        log.warn("400 VALIDATION | {} | {}", request.getRequestURI(), fieldErrors);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Validation failed", fieldErrors));
    }


    // ========================================================================
    // 5. GALAT JSON BODY  ->  400
    // ========================================================================
    /**
     * Jab request ka JSON hi toota hua ho.
     *
     * Example:
     *   - Comma missing
     *   - "status" : "ACTIVEE"   (enum me ye value hai hi nahi)
     *   - date ka format galat
     *
     * Asli exception message bahut lamba aur technical hota hai,
     * isliye user ko simple message de rahe hain.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadableJson(
            HttpMessageNotReadableException ex, HttpServletRequest request) {

        log.warn("400 BAD JSON | {} | {}", request.getRequestURI(), ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(
                        "Request body padhi nahi ja saki. JSON format ya enum value check karo."));
    }


    // ========================================================================
    // 6. MISSING QUERY PARAM  ->  400
    // ========================================================================
    /**
     * Example: search API me ?trainerName= bhejna bhool gaye.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParam(
            MissingServletRequestParameterException ex, HttpServletRequest request) {

        String msg = String.format("Required parameter missing : '%s'", ex.getParameterName());

        log.warn("400 | {} | {}", request.getRequestURI(), msg);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(msg));
    }


    // ========================================================================
    // 7. GALAT TYPE KA PARAM  ->  400
    // ========================================================================
    /**
     * Example: /api/batches/abc   (yahan Long id chahiye thi)
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {

        String msg = String.format("Parameter '%s' ki value galat type ki hai : '%s'",
                ex.getName(), ex.getValue());

        log.warn("400 | {} | {}", request.getRequestURI(), msg);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(msg));
    }


    // ========================================================================
    // 8. DATABASE CONSTRAINT TOOTA  ->  409
    // ========================================================================
    /**
     * Ye SAFETY NET hai.
     *
     * Normally service layer pehle hi check kar leti hai
     * ki email duplicate to nahi. Lekin do request EK SAATH
     * aa jayein (race condition) to dono check pass kar
     * jayengi aur DB pe jaake unique constraint tootega.
     *
     * Ye ismein wahi pakadta hai.
     *
     * Iss project me ye lagega:
     *   - trainers.email / username duplicate
     *   - student_batch me duplicate enrollment
     *   - foreign key delete karne pe
     *
     * NOTE: asli DB error message user ko NAHI dikhate —
     * usme table/column ke naam hote hain, wo security risk hai.
     * Isliye log me pura message, response me simple.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(
            DataIntegrityViolationException ex, HttpServletRequest request) {

        log.error("409 DB CONSTRAINT | {} | {}", request.getRequestURI(), ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(
                        "Database constraint fail hui. Duplicate data ho sakta hai, ya related record exist karta hai."));
    }


    // ========================================================================
    // 9. FILE BAHUT BADI  ->  413
    // ========================================================================
    /**
     * API 9 (Content) aur API 12 (Group PDF) ke liye zaroori.
     *
     * Size limit application.properties me set hogi:
     *   spring.servlet.multipart.max-file-size=10MB
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleFileTooLarge(
            MaxUploadSizeExceededException ex, HttpServletRequest request) {

        log.warn("413 FILE TOO LARGE | {}", request.getRequestURI());

        return ResponseEntity
                .status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ApiResponse.error("File bahut badi hai. Allowed limit se chhoti file bhejo."));
    }


    // ========================================================================
    // 10. GALAT HTTP METHOD  ->  405
    // ========================================================================
    /**
     * Example: POST endpoint pe GET maar diya.
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {

        String msg = String.format("'%s' method iss endpoint pe allowed nahi hai.", ex.getMethod());

        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse.error(msg));
    }


    // ========================================================================
    // 11. URL HI EXIST NAHI KARTA  ->  404
    // ========================================================================
    /**
     * Ye tabhi chalega jab application.properties me ye ho:
     *   spring.mvc.throw-exception-if-no-handler-found=true
     *
     * Warna Spring apna default white-label page dikhata hai.
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoHandler(
            NoHandlerFoundException ex) {

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("Endpoint nahi mila : " + ex.getRequestURL()));
    }


    // ========================================================================
    // 12. BAAKI SAB KUCH  ->  500
    // ========================================================================
    /**
     * SABSE LAST me. Jo upar kisi se match na kare wo yahan aayega.
     *
     * NullPointerException, database down, code me bug — sab.
     *
     * DHYAN DO:
     * ---------------------------------------------------------------------
     * User ko ex.getMessage() NAHI bhej rahe.
     *
     * Kyun? Kyunki usme stack trace, table ke naam, ya file path
     * ho sakta hai — attacker ke liye ye useful information hai.
     *
     * Isliye:
     *   log me  -> pura exception (developer ke liye)
     *   user ko -> generic message
     *
     * log.error me teesra parameter "ex" pass kiya hai —
     * isse console me PURA stack trace print hoga.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleAll(
            Exception ex, HttpServletRequest request) {

        log.error("500 INTERNAL ERROR | {} | {}", request.getRequestURI(), ex.getMessage(), ex);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Kuch galat ho gaya. Thodi der baad try karo."));
    }
}