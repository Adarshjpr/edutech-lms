package com.uncodemy.lms.controller;

import com.uncodemy.lms.dto.ApiResponse;
import com.uncodemy.lms.dto.request.StudentCreateRequest;
import com.uncodemy.lms.dto.request.StudentEnrollRequest;
import com.uncodemy.lms.dto.response.StudentResponse;
import com.uncodemy.lms.model.enums.ApprovalStatus;
import com.uncodemy.lms.model.enums.StudentStatus;
import com.uncodemy.lms.service.rule.StudentService;

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
 * AdminStudentController   ---  API 5 + 7 (admin side)
 * ============================================================================
 *
 * POST   /api/admin/students                   -> naya student (APPROVED)
 * POST   /api/admin/students/enroll            -> batch me add
 * GET    /api/admin/students                   -> list
 * GET    /api/admin/students/search            -> search
 * GET    /api/admin/students/{studentId}       -> ek student
 * GET    /api/admin/students/batch/{batchId}   -> batch ke students
 * PATCH  /api/admin/students/{id}/status       -> ACTIVE / BLOCKED
 * DELETE /api/admin/students/{id}/batch/{bid}  -> batch se hatao
 * ============================================================================
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/students")
@RequiredArgsConstructor
@Validated
public class AdminStudentController {

    private final StudentService studentService;


    // ========================================================================
    // API 7 --- CREATE
    // ========================================================================
    /**
     * POST /api/admin/students?adminId=ADM101
     *
     * {
     *   "name"    : "Adarsh Jha",
     *   "email"   : "adarsh@gmail.com",
     *   "phone"   : "9876543210",
     *   "course"  : "Java Full Stack",
     *   "batchId" : "JAVA101"       <- optional
     * }
     *
     * Student turant APPROVED hoga, welcome mail jayegi.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<StudentResponse>> createStudent(
            @Valid @RequestBody StudentCreateRequest request,
            @RequestParam @NotBlank(message = "adminId zaroori hai") String adminId) {

        log.info("POST /api/admin/students | adminId={} | email={}", adminId, request.getEmail());

        StudentResponse response = studentService.createByAdmin(request, adminId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Student add ho gaya. Welcome mail bhej di gayi hai.", response));
    }


    // ========================================================================
    // API 5 --- ENROLL  (pehle se bana student)
    // ========================================================================
    /**
     * POST /api/admin/students/enroll?adminId=ADM101
     *
     * {
     *   "studentId" : "STU101",
     *   "batchId"   : "MERN201"
     * }
     *
     * Ek student multiple batches me ho sakta hai.
     */
    @PostMapping("/enroll")
    public ResponseEntity<ApiResponse<StudentResponse>> enrollStudent(
            @Valid @RequestBody StudentEnrollRequest request,
            @RequestParam @NotBlank String adminId) {

        log.info("POST enroll | adminId={} | studentId={} | batchId={}",
                adminId, request.getStudentId(), request.getBatchId());

        StudentResponse response = studentService.enrollByAdmin(request, adminId);

        return ResponseEntity.ok(
                ApiResponse.success("Student batch me add ho gaya", response));
    }


    // ========================================================================
    // LIST
    // ========================================================================
    /**
     * GET /api/admin/students
     * GET /api/admin/students?approvalStatus=PENDING   <- jo approve nahi hue
     * GET /api/admin/students?status=BLOCKED
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<StudentResponse>>> getAllStudents(
            @RequestParam(required = false) ApprovalStatus approvalStatus,
            @RequestParam(required = false) StudentStatus status,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC)
            Pageable pageable) {

        Page<StudentResponse> students =
                studentService.getAllStudents(approvalStatus, status, pageable);

        return ResponseEntity.ok(
                ApiResponse.success("Students fetched successfully", students));
    }


    // ========================================================================
    // SEARCH
    // ========================================================================
    /**
     * GET /api/admin/students/search?q=adarsh
     *
     * Naam, email ya course — teeno me dhundhta hai.
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<StudentResponse>>> searchStudents(
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {

        Page<StudentResponse> results = studentService.searchStudents(q, pageable);

        return ResponseEntity.ok(ApiResponse.success("Search completed", results));
    }


    // ========================================================================
    // BATCH KE STUDENTS
    // ========================================================================
    /**
     * GET /api/admin/students/batch/JAVA101
     *
     * ⚠️ "/batch/..." ko "/{studentId}" se PEHLE rakha hai.
     */
    @GetMapping("/batch/{batchId}")
    public ResponseEntity<ApiResponse<Page<StudentResponse>>> getStudentsByBatch(
            @PathVariable String batchId,
            @PageableDefault(size = 50, sort = "id") Pageable pageable) {

        Page<StudentResponse> students = studentService.getStudentsByBatch(batchId, pageable);

        return ResponseEntity.ok(
                ApiResponse.success("Batch students fetched successfully", students));
    }


    // ========================================================================
    // EK STUDENT
    // ========================================================================
    /**
     * GET /api/admin/students/STU101
     *
     * Response me uske saare batches bhi aayenge.
     */
    @GetMapping("/{studentId}")
    public ResponseEntity<ApiResponse<StudentResponse>> getStudent(
            @PathVariable String studentId) {

        StudentResponse student = studentService.getByStudentId(studentId);

        return ResponseEntity.ok(
                ApiResponse.success("Student fetched successfully", student));
    }


    // ========================================================================
    // STATUS UPDATE
    // ========================================================================
    /**
     * PATCH /api/admin/students/STU101/status?status=BLOCKED
     */
    @PatchMapping("/{studentId}/status")
    public ResponseEntity<ApiResponse<StudentResponse>> updateStatus(
            @PathVariable String studentId,
            @RequestParam StudentStatus status) {

        StudentResponse student = studentService.updateStatus(studentId, status);

        return ResponseEntity.ok(
                ApiResponse.success("Student status update ho gaya", student));
    }


    // ========================================================================
    // BATCH SE HATAO
    // ========================================================================
    /**
     * DELETE /api/admin/students/STU101/batch/JAVA101
     *
     * Student delete NAHI hota — sirf iss batch se hat jata hai.
     * Doosre batches me rahega.
     */
    @DeleteMapping("/{studentId}/batch/{batchId}")
    public ResponseEntity<ApiResponse<Void>> removeFromBatch(
            @PathVariable String studentId,
            @PathVariable String batchId) {

        studentService.removeFromBatch(studentId, batchId);

        return ResponseEntity.ok(
                ApiResponse.success("Student ko batch se hata diya gaya"));
    }
}