package com.uncodemy.lms.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.*;

/**
 * ============================================================================
 * StudentEnrollRequest   ---  API 5 ka INPUT
 * ============================================================================
 *
 * "batch me student add kar sake — admin bhi, trainer bhi"
 *
 * POST /api/admin/students/enroll
 * POST /api/trainer/students/enroll
 *
 * {
 *   "studentId" : "STU101",
 *   "batchId"   : "JAVA101",
 *   "note"      : "Transfer from morning batch"
 * }
 *
 * CREATE SE FARAK
 * ---------------------------------------------------------------------------
 * StudentCreateRequest -> NAYA student banata hai
 * StudentEnrollRequest -> PEHLE SE BANE student ko batch me daalta hai
 *
 * Ek student multiple batches me ho sakta hai
 * (Java bhi padh raha, Spring bhi) — isliye alag API chahiye.
 * ============================================================================
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentEnrollRequest {

    @NotBlank(message = "Student ID zaroori hai")
    @Size(max = 20)
    private String studentId;

    @NotBlank(message = "Batch ID zaroori hai")
    @Size(max = 20)
    private String batchId;

    /**
     * Trainer ka note admin ke liye (approval me dikhega).
     * Admin ke case me ignore hota hai.
     */
    @Size(max = 500)
    private String note;
}