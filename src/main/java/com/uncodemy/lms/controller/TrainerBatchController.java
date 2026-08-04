package com.uncodemy.lms.controller;

import com.uncodemy.lms.dto.ApiResponse;
import com.uncodemy.lms.dto.request.BatchCreateRequest;
import com.uncodemy.lms.dto.response.ApprovalRequestResponse;
import com.uncodemy.lms.dto.response.BatchResponse;
import com.uncodemy.lms.model.enums.ApprovalStatus;
import com.uncodemy.lms.service.rule.ApprovalService;
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
 * TrainerBatchController   ---  API 4 + 6
 * ============================================================================
 *
 * POST  /api/trainer/batches                  -> batch create (PENDING)
 * GET   /api/trainer/batches                  -> mere batches
 * GET   /api/trainer/batches/my-requests      -> meri approval requests
 * PATCH /api/trainer/batches/{batchId}/topic  -> topic update (sirf apne batch ka)
 *
 * ADMIN CONTROLLER SE FARAK
 * ---------------------------------------------------------------------------
 * Har endpoint me "trainerId" zaroori hai, aur har jagah
 * OWNERSHIP CHECK hota hai.
 *
 * Trainer sirf apne batch ke saath khel sakta hai —
 * doosre trainer ke batch ko touch nahi kar sakta.
 * ============================================================================
 */
@Slf4j
@RestController
@RequestMapping("/api/trainer/batches")
@RequiredArgsConstructor
@Validated
public class TrainerBatchController {

    private final BatchService batchService;
    private final ApprovalService approvalService;


    // ========================================================================
    // API 4 --- CREATE  (PENDING -> admin approve karega)
    // ========================================================================
    /**
     * POST /api/trainer/batches?trainerId=TR101&note=Naya evening batch
     *
     * BODY: same as admin ka
     * {
     *   "batchName" : "Java Advanced",
     *   "timing"    : "8 PM - 10 PM",
     *   "startDate" : "2026-08-10"
     * }
     *
     * NOTE: body me trainerId bheja bhi to IGNORE hoga —
     * trainer khud hi apne batch ka trainer banega.
     *
     * KYA HOGA:
     * -----------------------------------------------------------------------
     *   1. Batch banega -> approvalStatus = PENDING
     *   2. ApprovalRequest banegi
     *   3. Saare admins ko mail
     *   4. Response me batch (PENDING status ke saath)
     *
     * Batch kisi public list me nahi dikhega jab tak
     * admin approve na kare.
     *
     * 201 hi de rahe hain kyunki resource ban to gayi hai,
     * bas activate hone ka intezaar hai.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<BatchResponse>> createBatch(
            @Valid @RequestBody BatchCreateRequest request,
            @RequestParam @NotBlank(message = "trainerId zaroori hai") String trainerId,
            @RequestParam(required = false) String note) {

        log.info("POST /api/trainer/batches | trainerId={} | batchName={}",
                trainerId, request.getBatchName());

        BatchResponse response = batchService.createByTrainer(request, trainerId, note);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Batch create request bhej di gayi hai. Admin ke approve karne ka intezaar karein.",
                        response));
    }


    // ========================================================================
    // MERE BATCHES
    // ========================================================================
    /**
     * GET /api/trainer/batches?trainerId=TR101
     * GET /api/trainer/batches?trainerId=TR101&onlyApproved=false
     *
     * onlyApproved
     * -----------------------------------------------------------------------
     *   true  (default) -> sirf approved batches (padha rahe hain)
     *   false           -> PENDING aur REJECTED bhi dikhenge
     *
     * false wala isliye taaki trainer dekh sake ki uski
     * request ka kya hua.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<BatchResponse>>> getMyBatches(
            @RequestParam @NotBlank String trainerId,
            @RequestParam(defaultValue = "true") boolean onlyApproved,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC)
            Pageable pageable) {

        Page<BatchResponse> batches =
                batchService.getBatchesByTrainer(trainerId, onlyApproved, pageable);

        return ResponseEntity.ok(
                ApiResponse.success("Batches fetched successfully", batches));
    }


    // ========================================================================
    // MERI APPROVAL REQUESTS
    // ========================================================================
    /**
     * GET /api/trainer/batches/my-requests?trainerId=TR101
     * GET /api/trainer/batches/my-requests?trainerId=TR101&status=PENDING
     *
     * Trainer dekh sakta hai:
     *   - Kaunsi request abhi pending hai
     *   - Kya approve hua
     *   - Kya reject hua AUR KYUN (adminRemark me reason)
     *
     * ⚠️ Ye endpoint "/{batchId}" wale se PEHLE likha hai
     * (agar wo hota to). Fixed paths hamesha upar rakho —
     * warna "my-requests" ko batchId samajh liya jayega.
     */
    @GetMapping("/my-requests")
    public ResponseEntity<ApiResponse<Page<ApprovalRequestResponse>>> getMyRequests(
            @RequestParam @NotBlank String trainerId,
            @RequestParam(required = false) ApprovalStatus status,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC)
            Pageable pageable) {

        Page<ApprovalRequestResponse> requests =
                approvalService.getMyRequests(trainerId, status, pageable);

        return ResponseEntity.ok(
                ApiResponse.success("Requests fetched successfully", requests));
    }


    // ========================================================================
    // API 6 --- TOPIC UPDATE  (sirf apne batch ka)
    // ========================================================================
    /**
     * PATCH /api/trainer/batches/JAVA101/topic?trainerId=TR101&topic=Spring Security
     *
     * ADMIN SE FARAK
     * -----------------------------------------------------------------------
     * Yahan trainerId bhej rahe hain, matlab service
     * OWNERSHIP CHECK karegi.
     *
     * Batch trainer ka nahi hai to:
     *   400 -> "Ye batch aapka nahi hai. batchId : JAVA101"
     *
     * Bina iss check ke Trainer A, Trainer B ke batch ka
     * topic badal deta.
     */
    @PatchMapping("/{batchId}/topic")
    public ResponseEntity<ApiResponse<BatchResponse>> updateTopic(
            @PathVariable String batchId,
            @RequestParam @NotBlank String trainerId,
            @RequestParam @NotBlank(message = "Topic khali nahi ho sakta") String topic) {

        log.info("PATCH topic (trainer) | batchId={} | trainerId={} | topic={}",
                batchId, trainerId, topic);

        // trainerId pass kar rahe hain -> ownership check hoga
        BatchResponse batch = batchService.updateCurrentTopic(batchId, topic, trainerId);

        return ResponseEntity.ok(
                ApiResponse.success("Current topic update ho gaya", batch));
    }
}