package com.uncodemy.lms.controller;

import com.uncodemy.lms.dto.ApiResponse;
import com.uncodemy.lms.dto.request.ApprovalActionRequest;
import com.uncodemy.lms.dto.response.ApprovalRequestResponse;
import com.uncodemy.lms.model.enums.ApprovalStatus;
import com.uncodemy.lms.model.enums.RequestType;
import com.uncodemy.lms.service.rule.ApprovalService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * ============================================================================
 * AdminApprovalController   ---  API 4 ka approval hissa
 * ============================================================================
 *
 * GET  /api/admin/approvals                -> pending requests
 * GET  /api/admin/approvals/count          -> badge ke liye count
 * GET  /api/admin/approvals/{id}           -> ek request
 * POST /api/admin/approvals/{id}/approve   -> approve
 * POST /api/admin/approvals/{id}/reject    -> reject (reason zaroori)
 *
 * YE CONTROLLER BATCH AUR STUDENT DONO KE LIYE HAI
 * ---------------------------------------------------------------------------
 * ApprovalRequest ek generic table hai, isliye Phase 4 me
 * student requests bhi ISI endpoint pe aayengi.
 *
 * Alag controller banane ki zarurat nahi padegi.
 * ============================================================================
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/approvals")
@RequiredArgsConstructor
public class AdminApprovalController {

    private final ApprovalService approvalService;


    // ========================================================================
    // LIST  ---  admin dashboard ki main screen
    // ========================================================================
    /**
     * GET /api/admin/approvals
     * GET /api/admin/approvals?status=APPROVED
     * GET /api/admin/approvals?requestType=BATCH_CREATE
     *
     * status na diya to PENDING default hai —
     * kyunki admin ko yahi dekhna hota hai.
     *
     * RESPONSE:
     * {
     *   "data": {
     *     "content": [
     *       {
     *         "id": 5,
     *         "requestType": "BATCH_CREATE",
     *         "status": "PENDING",
     *         "trainerName": "Rahul Sharma",
     *         "batchName": "Java Advanced",
     *         "requestNote": "Naya evening batch chahiye",
     *         "createdAt": "2026-07-31 18:30:00"
     *       }
     *     ],
     *     "totalElements": 3
     *   }
     * }
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<ApprovalRequestResponse>>> getRequests(
            @RequestParam(required = false) ApprovalStatus status,
            @RequestParam(required = false) RequestType requestType,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC)
            Pageable pageable) {

        Page<ApprovalRequestResponse> requests =
                approvalService.getRequests(status, requestType, pageable);

        return ResponseEntity.ok(
                ApiResponse.success("Requests fetched successfully", requests));
    }


    // ========================================================================
    // COUNT  ---  dashboard badge
    // ========================================================================
    /**
     * GET /api/admin/approvals/count
     *
     * RESPONSE:
     * { "data" : { "pending" : 3 } }
     *
     * Frontend isse "Approvals (3)" wala laal badge dikhayega.
     *
     * Map return kar rahe hain taaki aage aur counts
     * add kar sakein (batchPending, studentPending)
     * bina response ka structure tode.
     */
    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getPendingCount() {

        long pending = approvalService.countPending();

        return ResponseEntity.ok(
                ApiResponse.success("Count fetched", Map.of("pending", pending)));
    }


    // ========================================================================
    // EK REQUEST
    // ========================================================================
    /**
     * GET /api/admin/approvals/5
     *
     * ⚠️ Ye "/count" ke BAAD likha hai.
     *
     * Kyun? Kyunki "/{id}" kisi bhi cheez se match karta hai.
     * "/count" upar na hota to Spring "count" ko id samajhne
     * ki koshish karta aur type mismatch error deta.
     */
    @GetMapping("/{requestId}")
    public ResponseEntity<ApiResponse<ApprovalRequestResponse>> getRequest(
            @PathVariable Long requestId) {

        ApprovalRequestResponse request = approvalService.getById(requestId);

        return ResponseEntity.ok(
                ApiResponse.success("Request fetched successfully", request));
    }


    // ========================================================================
    // APPROVE
    // ========================================================================
    /**
     * POST /api/admin/approvals/5/approve
     *
     * BODY:
     * {
     *   "adminId" : "ADM101",
     *   "remark"  : "Approved, timing theek hai"      <- optional
     * }
     *
     * KYA HOGA:
     *   1. Request -> APPROVED
     *   2. Batch   -> APPROVED (ab list me dikhega)
     *   3. Trainer ko mail
     *
     * Pehle se approve/reject ho chuki hai to:
     *   400 -> "Ye request pehle se APPROVED ho chuki hai"
     *
     * POST kyun, PATCH kyun nahi?
     * -----------------------------------------------------------------------
     * PATCH field update karne ke liye hota hai.
     *
     * Ye sirf field update nahi hai — ye ek ACTION hai
     * jisme mail jati hai, doosri entity badalti hai.
     *
     * Aise action-based endpoints ke liye POST sahi hai.
     */
    @PostMapping("/{requestId}/approve")
    public ResponseEntity<ApiResponse<ApprovalRequestResponse>> approve(
            @PathVariable Long requestId,
            @Valid @RequestBody ApprovalActionRequest action) {

        log.info("POST approve | requestId={} | adminId={}", requestId, action.getAdminId());

        ApprovalRequestResponse response = approvalService.approve(requestId, action);

        return ResponseEntity.ok(
                ApiResponse.success("Request approve ho gayi. Trainer ko mail bhej di gayi hai.",
                        response));
    }


    // ========================================================================
    // REJECT
    // ========================================================================
    /**
     * POST /api/admin/approvals/5/reject
     *
     * BODY:
     * {
     *   "adminId" : "ADM101",
     *   "remark"  : "Is naam ka batch already chal raha hai"   <- ZAROORI
     * }
     *
     * remark khali bheja to:
     *   400 -> "Reject karne ke liye reason (remark) dena zaroori hai"
     *
     * KYA HOGA:
     *   1. Request -> REJECTED
     *   2. Batch   -> REJECTED (kahin nahi dikhega)
     *   3. Trainer ko mail --- REASON ke saath
     *
     * Batch ki row DELETE nahi hoti — history rehti hai.
     */
    @PostMapping("/{requestId}/reject")
    public ResponseEntity<ApiResponse<ApprovalRequestResponse>> reject(
            @PathVariable Long requestId,
            @Valid @RequestBody ApprovalActionRequest action) {

        log.info("POST reject | requestId={} | adminId={}", requestId, action.getAdminId());

        ApprovalRequestResponse response = approvalService.reject(requestId, action);

        return ResponseEntity.ok(
                ApiResponse.success("Request reject kar di gayi. Trainer ko mail bhej di gayi hai.",
                        response));
    }
}