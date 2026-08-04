package com.uncodemy.lms.controller;

import com.uncodemy.lms.dto.ApiResponse;
import com.uncodemy.lms.dto.request.BatchCreateRequest;
import com.uncodemy.lms.dto.response.BatchResponse;
import com.uncodemy.lms.model.enums.BatchStatus;
import com.uncodemy.lms.service.rule.BatchService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * ============================================================================
 * AdminBatchController   ---  API 3 + 6 + 10(base)
 * ============================================================================
 *
 * POST   /api/admin/batches                  -> batch create (seedha APPROVED)
 * GET    /api/admin/batches                  -> saare approved batches
 * GET    /api/admin/batches/search           -> trainer naam / topic se
 * GET    /api/admin/batches/{batchId}        -> ek batch
 * PATCH  /api/admin/batches/{batchId}/topic  -> current topic update
 * PATCH  /api/admin/batches/{batchId}/status -> ACTIVE / COMPLETED
 *
 * @Validated (class level)
 * ---------------------------------------------------------------------------
 * Ye @Valid se ALAG hai.
 *
 *   @Valid     -> @RequestBody (DTO) ki validation
 *   @Validated -> @RequestParam / @PathVariable ki validation
 *
 * Neeche @NotBlank direct parameter pe laga hai —
 * wo iske bina kaam nahi karega.
 * ============================================================================
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/batches")
@RequiredArgsConstructor
@Validated
public class AdminBatchController {

    private final BatchService batchService;


    // ========================================================================
    // API 3 --- CREATE  (seedha APPROVED)
    // ========================================================================
    /**
     * POST /api/admin/batches?adminId=ADM101
     *
     * BODY:
     * {
     *   "batchName"    : "Java Full Stack",
     *   "timing"       : "7 PM - 9 PM",
     *   "trainerId"    : "TR101",
     *   "meetLink"     : "https://meet.google.com/abc-defg-hij",
     *   "startDate"    : "2026-08-05",
     *   "status"       : "UPCOMING"
     * }
     *
     * RESPONSE (201): batchId "JAVA101" ke saath, approvalStatus = APPROVED
     *
     * adminId QUERY PARAM me kyun?
     * -----------------------------------------------------------------------
     * Kyunki abhi login nahi hai — server ko pata nahi
     * kaun request bhej raha.
     *
     * Security aane par ye HAT JAYEGA, JWT se apne aap
     * pata chal jayega.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<BatchResponse>> createBatch(
            @Valid @RequestBody BatchCreateRequest request,
            @RequestParam @NotBlank(message = "adminId zaroori hai") String adminId) {

        log.info("POST /api/admin/batches | adminId={} | batchName={}",
                adminId, request.getBatchName());

        BatchResponse response = batchService.createByAdmin(request, adminId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Batch created successfully", response));
    }


    // ========================================================================
    // LIST
    // ========================================================================
    /**
     * GET /api/admin/batches
     * GET /api/admin/batches?status=ACTIVE
     * GET /api/admin/batches?page=0&size=20
     *
     * Sirf APPROVED batches aayenge.
     * PENDING wale approval dashboard me dikhenge.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<BatchResponse>>> getAllBatches(
            @RequestParam(required = false) BatchStatus status,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC)
            Pageable pageable) {

        Page<BatchResponse> batches = batchService.getAllApprovedBatches(status, pageable);

        return ResponseEntity.ok(
                ApiResponse.success("Batches fetched successfully", batches));
    }


    // ========================================================================
    // API 10 --- SEARCH
    // ========================================================================
    /**
     * GET /api/admin/batches/search?trainerName=rahul
     * GET /api/admin/batches/search?topic=spring
     * GET /api/admin/batches/search?trainerName=rahul&topic=spring
     *
     * Dono parameter optional. Kuch na do to saare batches.
     *
     * API 10 ki main requirement:
     *   "admin trainer ke naam likhe aur aa jaye"
     *   "topic ke hisaab se search kar sake"
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<BatchResponse>>> searchBatches(
            @RequestParam(required = false) String trainerName,
            @RequestParam(required = false) String topic,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC)
            Pageable pageable) {

        Page<BatchResponse> results = batchService.searchBatches(trainerName, topic, pageable);

        return ResponseEntity.ok(
                ApiResponse.success("Search completed", results));
    }


    // ========================================================================
    // EK BATCH
    // ========================================================================
    /**
     * GET /api/admin/batches/JAVA101
     */
    @GetMapping("/{batchId}")
    public ResponseEntity<ApiResponse<BatchResponse>> getBatch(
            @PathVariable String batchId) {

        BatchResponse batch = batchService.getByBatchId(batchId);

        return ResponseEntity.ok(
                ApiResponse.success("Batch fetched successfully", batch));
    }


    // ========================================================================
    // API 6 --- CURRENT TOPIC UPDATE
    // ========================================================================
    /**
     * PATCH /api/admin/batches/JAVA101/topic?topic=Spring Boot Security
     *
     * Admin kisi bhi batch ka topic update kar sakta hai —
     * ownership check nahi hoga (wo sirf trainer ke liye hai).
     *
     * Isliye service ko trainerId = null bhej rahe hain.
     */
    @PatchMapping("/{batchId}/topic")
    public ResponseEntity<ApiResponse<BatchResponse>> updateTopic(
            @PathVariable String batchId,
            @RequestParam @NotBlank(message = "Topic khali nahi ho sakta") String topic) {

        log.info("PATCH topic (admin) | batchId={} | topic={}", batchId, topic);

        // null = admin kar raha hai, ownership check skip
        BatchResponse batch = batchService.updateCurrentTopic(batchId, topic, null);

        return ResponseEntity.ok(
                ApiResponse.success("Current topic update ho gaya", batch));
    }


    // ========================================================================
    // BATCH STATUS UPDATE
    // ========================================================================
    /**
     * PATCH /api/admin/batches/JAVA101/status?status=ACTIVE
     *
     * UPCOMING -> ACTIVE -> COMPLETED
     */
    @PatchMapping("/{batchId}/status")
    public ResponseEntity<ApiResponse<BatchResponse>> updateStatus(
            @PathVariable String batchId,
            @RequestParam BatchStatus status) {

        log.info("PATCH status | batchId={} | status={}", batchId, status);

        BatchResponse batch = batchService.updateStatus(batchId, status);

        return ResponseEntity.ok(
                ApiResponse.success("Batch status update ho gaya", batch));
    }
}