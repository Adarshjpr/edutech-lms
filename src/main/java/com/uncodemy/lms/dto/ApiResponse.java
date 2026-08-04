package com.uncodemy.lms.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * ============================================================================
 * ApiResponse<T>   ---  Common Response Wrapper
 * ============================================================================
 *
 * Project ki HAR API isi format me response degi.
 *
 * SUCCESS ka response:
 * ---------------------------------------------------------------------------
 * {
 *   "success"   : true,
 *   "message"   : "Trainer created successfully",
 *   "data"      : { "trainerId": "TR101", "name": "Rahul Sharma" },
 *   "timestamp" : "2026-07-31T18:30:00"
 * }
 *
 * ERROR ka response:
 * ---------------------------------------------------------------------------
 * {
 *   "success"   : false,
 *   "message"   : "Trainer not found with id : TR999",
 *   "errors"    : { "email": "Email is required" },
 *   "timestamp" : "2026-07-31T18:30:00"
 * }
 *
 * FAYDA
 * ---------------------------------------------------------------------------
 * ✔ Frontend ko hamesha "success" field check karni hai, bas
 * ✔ Error aur success ka structure same rehta hai
 * ✔ Naya field add karna ho to sirf yahan add karo, sab jagah aa jayega
 *
 * <T> KYA HAI?
 * ---------------------------------------------------------------------------
 * T = Generic Type. Matlab "data" ke andar kuch bhi aa sakta hai.
 *
 *   ApiResponse<TrainerResponse>        -> ek trainer
 *   ApiResponse<List<BatchResponse>>    -> batch ki list
 *   ApiResponse<Void>                   -> sirf message, koi data nahi
 *
 * @JsonInclude(NON_NULL)
 * ---------------------------------------------------------------------------
 * Jo field null hai wo JSON me dikhegi hi nahi.
 * Success me "errors" null hota hai -> response me aayega hi nahi.
 * Isse response clean rehta hai.
 * ============================================================================
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    /**
     * Kaam hua ya nahi.
     * true  -> sab theek
     * false -> koi error aayi
     */
    private boolean success;

    /**
     * Insaan ke padhne layak message.
     *
     * Example:
     * "Trainer created successfully"
     * "Batch not found with id : JAVA999"
     */
    private String message;

    /**
     * Asli data.
     *
     * Error ke case me null rahega
     * (aur @JsonInclude ki wajah se JSON me dikhega bhi nahi).
     */
    private T data;

    /**
     * Validation errors.
     *
     * GlobalExceptionHandler isme field-wise error bharega:
     *
     * {
     *   "email" : "Email is required",
     *   "name"  : "Name cannot be blank"
     * }
     *
     * Type Object rakha hai taaki Map ya List dono aa sakein.
     */
    private Object errors;

    /**
     * Response kab bana.
     * Debugging aur logging me kaam aata hai.
     */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();


    // ========================================================================
    // STATIC HELPER METHODS
    // ------------------------------------------------------------------------
    // Inki wajah se controller me har baar builder likhna nahi padega.
    //
    // Bina helper ke :
    //   return ResponseEntity.ok(
    //       ApiResponse.<TrainerResponse>builder()
    //           .success(true).message("...").data(res).build());
    //
    // Helper ke saath :
    //   return ResponseEntity.ok(ApiResponse.success("...", res));
    // ========================================================================

    /**
     * Success + message + data
     *
     * Example:
     * ApiResponse.success("Trainer created successfully", trainerResponse)
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    /**
     * Success + sirf message (koi data nahi)
     *
     * Example:
     * ApiResponse.success("Batch approved successfully")
     */
    public static <T> ApiResponse<T> success(String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .build();
    }

    /**
     * Error + sirf message
     *
     * Example:
     * ApiResponse.error("Trainer not found with id : TR999")
     */
    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .build();
    }

    /**
     * Error + message + field-wise errors
     *
     * Ye mostly GlobalExceptionHandler use karega
     * jab @Valid ki validation fail hoti hai.
     */
    public static <T> ApiResponse<T> error(String message, Object errors) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .errors(errors)
                .build();
    }
}