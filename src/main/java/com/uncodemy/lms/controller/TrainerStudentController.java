package com.uncodemy.lms.controller;

import com.uncodemy.lms.dto.ApiResponse;
import com.uncodemy.lms.dto.request.StudentCreateRequest;
import com.uncodemy.lms.dto.request.StudentEnrollRequest;
import com.uncodemy.lms.dto.response.StudentResponse;
import com.uncodemy.lms.service.rule.BatchService;
import com.uncodemy.lms.service.rule.StudentService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * ============================================================================
 * TrainerStudentController   ---  API 5 + 7 (trainer side)
 * ============================================================================
 *
 * POST /api/trainer/students                 -> naya student (PENDING)
 * POST /api/trainer/students/enroll          -> batch me add (PENDING)
 * GET  /api/trainer/students/batch/{batchId} -> mere batch ke students
 *
 * ADMIN SE FARAK
 * ---------------------------------------------------------------------------
 * Har action pe OWNERSHIP CHECK hota hai — trainer sirf
 * apne batch me student add kar sakta hai.
 *
 * Aur har add ADMIN KE APPROVAL ka intezaar karta hai.
 * ============================================================================
 */
@Slf4j
@RestController
@RequestMapping("/api/trainer/students")
@RequiredArgsConstructor
@Validated
public class TrainerStudentController {

    private final StudentService studentService;
    private final BatchService batchService;


    // ========================================================================
    // API 7 --- CREATE  (PENDING)
    // ========================================================================
    /**
     * POST /api/trainer/students?trainerId=TR101&note=Walk-in admission
     *
     * {
     *   "name"    : "Priya Singh",
     *   "email"   : "priya@gmail.com",
     *   "course"  : "Java Full Stack",
     *   "batchId" : "JAVA101"      <- ZAROORI hai trainer ke liye
     * }
     *
     * KYA HOGA:
     *   1. Student banega -> PENDING
     *   2. ApprovalRequest banegi
     *   3. Admins ko mail
     *   4. Student ko KOI MAIL NAHI (approve hone par jayegi)
     *   5. Enrollment bhi approve hone par banegi
     *
     * Batch trainer ka nahi hai to 400 aayega.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<StudentResponse>> createStudent(
            @Valid @RequestBody StudentCreateRequest request,
            @RequestParam @NotBlank(message = "trainerId zaroori hai") String trainerId,
            @RequestParam(required = false) String note) {

        log.info("POST /api/trainer/students | trainerId={} | email={}",
                trainerId, request.getEmail());

        StudentResponse response = studentService.createByTrainer(request, trainerId, note);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Student add request bhej di gayi. Admin ke approve karne ka intezaar karein.",
                        response));
    }


    // ========================================================================
    // API 5 --- ENROLL  (PENDING)
    // ========================================================================
    /**
     * POST /api/trainer/students/enroll?trainerId=TR101
     *
     * {
     *   "studentId" : "STU101",
     *   "batchId"   : "JAVA101",
     *   "note"      : "Morning batch se transfer"
     * }
     *
     * Pehle se bana student apne batch me daalna.
     * Ye bhi approval ke baad hi hoga.
     */
    @PostMapping("/enroll")
    public ResponseEntity<ApiResponse<StudentResponse>> enrollStudent(
            @Valid @RequestBody StudentEnrollRequest request,
            @RequestParam @NotBlank String trainerId) {

        log.info("POST trainer enroll | trainerId={} | studentId={} | batchId={}",
                trainerId, request.getStudentId(), request.getBatchId());

        StudentResponse response = studentService.enrollByTrainer(request, trainerId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Enroll request bhej di gayi. Admin approve karega.",
                        response));
    }


    // ========================================================================
    // MERE BATCH KE STUDENTS
    // ========================================================================
    /**
     * GET /api/trainer/students/batch/JAVA101?trainerId=TR101
     *
     * Ownership check hoga — doosre trainer ke batch ke
     * students nahi dekh sakte.
     */
    @GetMapping("/batch/{batchId}")
    public ResponseEntity<ApiResponse<Page<StudentResponse>>> getMyBatchStudents(
            @PathVariable String batchId,
            @RequestParam @NotBlank String trainerId,
            @PageableDefault(size = 50, sort = "id") Pageable pageable) {

        // Batch trainer ka hai?
        batchService.verifyBatchBelongsToTrainer(batchId, trainerId);

        Page<StudentResponse> students = studentService.getStudentsByBatch(batchId, pageable);

        return ResponseEntity.ok(
                ApiResponse.success("Students fetched successfully", students));
    }
}