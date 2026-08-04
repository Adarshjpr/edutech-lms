package com.uncodemy.lms.service.impl;

import com.uncodemy.lms.dto.request.StudentCreateRequest;
import com.uncodemy.lms.dto.request.StudentEnrollRequest;
import com.uncodemy.lms.dto.response.StudentResponse;
import com.uncodemy.lms.exception.BadRequestException;
import com.uncodemy.lms.exception.DuplicateResourceException;
import com.uncodemy.lms.exception.ResourceNotFoundException;
import com.uncodemy.lms.model.*;
import com.uncodemy.lms.model.enums.ApprovalStatus;
import com.uncodemy.lms.model.enums.RequestType;
import com.uncodemy.lms.model.enums.StudentStatus;
import com.uncodemy.lms.repository.*;
import com.uncodemy.lms.service.rule.BatchService;
import com.uncodemy.lms.service.rule.EmailService;
import com.uncodemy.lms.service.rule.StudentService;
import com.uncodemy.lms.service.rule.TrainerService;
import com.uncodemy.lms.util.IdGeneratorUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

/**
 * ============================================================================
 * StudentServiceImpl   ---  API 5 + 7
 * ============================================================================
 *
 * CHAAR ENTRY POINTS, DO PATTERN
 * ---------------------------------------------------------------------------
 *
 *   createByAdmin  |  enrollByAdmin     -> turant, koi approval nahi
 *   createByTrainer|  enrollByTrainer   -> PENDING + ApprovalRequest + mail
 *
 * Common kaam private helpers me hai.
 * ============================================================================
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class StudentServiceImpl implements StudentService {

    private final StudentRepository studentRepository;
    private final StudentBatchRepository studentBatchRepository;
    private final ApprovalRequestRepository approvalRepository;
    private final AdminRepository adminRepository;
    private final TrainerService trainerService;
    private final BatchService batchService;
    private final EmailService emailService;


    // ========================================================================
    // API 7 --- ADMIN CREATE
    // ========================================================================
    @Override
    public StudentResponse createByAdmin(StudentCreateRequest request, String adminId) {

        log.info("Admin student create | adminId={} | email={}", adminId, request.getEmail());

        // Admin valid hai?
        adminRepository.findByAdminId(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin", "adminId", adminId));

        Student student = buildStudent(request, null);
        student.setApprovalStatus(ApprovalStatus.APPROVED);   // admin = seedha approved

        Student saved = studentRepository.save(student);

        // Batch diya hai to enroll bhi kar do
        String batchName = null;
        if (request.getBatchId() != null && !request.getBatchId().isBlank()) {
            Batch batch = batchService.getEntityOrThrow(request.getBatchId().trim());
            ensureBatchApproved(batch);
            createEnrollment(saved, batch);
            batchName = batch.getBatchName();
        }

        // Welcome mail — student APPROVED hai to turant
        emailService.sendStudentWelcome(
                saved.getEmail(), saved.getName(), saved.getStudentId(),
                batchName != null ? batchName : "Uncodemy");

        log.info("Student created by admin | studentId={}", saved.getStudentId());

        return buildFullResponse(saved);
    }


    // ========================================================================
    // API 7 --- TRAINER CREATE  (approval flow)
    // ========================================================================
    @Override
    public StudentResponse createByTrainer(StudentCreateRequest request, String trainerId, String note) {

        log.info("Trainer student create | trainerId={} | email={}", trainerId, request.getEmail());

        Trainer trainer = trainerService.getEntityOrThrow(trainerId);
        ensureTrainerActive(trainer);

        /*
         * Trainer ke liye batchId ZAROORI hai.
         *
         * Kyun? Kyunki trainer ka kaam hai apne batch me student
         * laana. Bina batch ke student banane ka koi matlab nahi —
         * aur admin ko approve karte waqt pata bhi nahi chalega
         * ki student kahan daalna hai.
         */
        if (request.getBatchId() == null || request.getBatchId().isBlank()) {
            throw new BadRequestException("Trainer ko student add karte waqt batchId dena zaroori hai");
        }

        String batchId = request.getBatchId().trim();

        // Batch trainer ka hai? (ownership check)
        batchService.verifyBatchBelongsToTrainer(batchId, trainerId);

        Batch batch = batchService.getEntityOrThrow(batchId);
        ensureBatchApproved(batch);

        // Student banao — PENDING
        Student student = buildStudent(request, trainer);
        student.setApprovalStatus(ApprovalStatus.PENDING);

        Student saved = studentRepository.save(student);

        /*
         * ENROLLMENT ABHI NAHI BANEGI.
         *
         * Kyun? Kyunki student abhi approve hi nahi hua.
         * Enrollment banate to wo batch ki list me dikhne lagta
         * aur announcement ki mail bhi chali jati.
         *
         * ApprovalRequest me batch save kar rahe hain —
         * approve hote hi enrollment ban jayegi.
         */
        ApprovalRequest approvalRequest = ApprovalRequest.builder()
                .requestType(RequestType.STUDENT_ADD)
                .status(ApprovalStatus.PENDING)
                .requestedByTrainer(trainer)
                .student(saved)
                .batch(batch)
                .requestNote(trim(note))
                .build();

        approvalRepository.save(approvalRequest);

        // Admins ko batao
        notifyAdmins(trainer.getName(), "Student Addition",
                saved.getName() + " -> " + batch.getBatchName());

        log.info("Student created by trainer | studentId={} | status=PENDING | requestId={}",
                saved.getStudentId(), approvalRequest.getId());

        return buildFullResponse(saved);
    }


    // ========================================================================
    // API 5 --- ADMIN ENROLL  (pehle se bana student)
    // ========================================================================
    @Override
    public StudentResponse enrollByAdmin(StudentEnrollRequest request, String adminId) {

        adminRepository.findByAdminId(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin", "adminId", adminId));

        Student student = getEntityOrThrow(request.getStudentId());
        Batch batch = batchService.getEntityOrThrow(request.getBatchId());

        ensureBatchApproved(batch);
        ensureStudentApproved(student);

        createEnrollment(student, batch);

        emailService.sendStudentWelcome(
                student.getEmail(), student.getName(),
                student.getStudentId(), batch.getBatchName());

        log.info("Student enrolled by admin | studentId={} | batchId={}",
                student.getStudentId(), batch.getBatchId());

        return buildFullResponse(student);
    }


    // ========================================================================
    // API 5 --- TRAINER ENROLL  (approval ke saath)
    // ========================================================================
    @Override
    public StudentResponse enrollByTrainer(StudentEnrollRequest request, String trainerId) {

        Trainer trainer = trainerService.getEntityOrThrow(trainerId);
        ensureTrainerActive(trainer);

        // Batch trainer ka hai?
        batchService.verifyBatchBelongsToTrainer(request.getBatchId(), trainerId);

        Student student = getEntityOrThrow(request.getStudentId());
        Batch batch = batchService.getEntityOrThrow(request.getBatchId());

        ensureBatchApproved(batch);
        ensureStudentApproved(student);

        // Pehle se enrolled to nahi?
        if (studentBatchRepository.existsByStudentAndBatch(student, batch)) {
            throw new BadRequestException(
                    "Student pehle se iss batch me hai : " + student.getStudentId());
        }

        // Duplicate request to nahi?
        if (approvalRepository.existsByStudentAndStatus(student, ApprovalStatus.PENDING)) {
            throw new BadRequestException(
                    "Iss student ki ek request pehle se pending hai");
        }

        ApprovalRequest approvalRequest = ApprovalRequest.builder()
                .requestType(RequestType.STUDENT_ADD)
                .status(ApprovalStatus.PENDING)
                .requestedByTrainer(trainer)
                .student(student)
                .batch(batch)
                .requestNote(trim(request.getNote()))
                .build();

        approvalRepository.save(approvalRequest);

        notifyAdmins(trainer.getName(), "Student Addition",
                student.getName() + " -> " + batch.getBatchName());

        log.info("Enroll request by trainer | studentId={} | batchId={} | requestId={}",
                student.getStudentId(), batch.getBatchId(), approvalRequest.getId());

        return buildFullResponse(student);
    }


    // ========================================================================
    // BATCH SE HATAO
    // ========================================================================
    @Override
    public void removeFromBatch(String studentId, String batchId) {

        Student student = getEntityOrThrow(studentId);
        Batch batch = batchService.getEntityOrThrow(batchId);

        StudentBatch enrollment = studentBatchRepository
                .findByStudentAndBatch(student, batch)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Enrollment", "student+batch", studentId + " + " + batchId));

        /*
         * Row DELETE nahi kar rahe — sirf active = false.
         *
         * Isse pata rehta hai ki student kabhi iss batch me tha,
         * aur kab chhoda.
         */
        enrollment.setActive(false);
        enrollment.setLeftAt(LocalDateTime.now());

        studentBatchRepository.save(enrollment);

        log.info("Student removed from batch | studentId={} | batchId={}", studentId, batchId);
    }


    // ========================================================================
    // READ
    // ========================================================================
    @Override
    @Transactional(readOnly = true)
    public StudentResponse getByStudentId(String studentId) {

        Student student = studentRepository.findByStudentIdWithTrainer(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", "studentId", studentId));

        return buildFullResponse(student);
    }


    @Override
    @Transactional(readOnly = true)
    public Page<StudentResponse> getAllStudents(ApprovalStatus approvalStatus,
                                                StudentStatus status,
                                                Pageable pageable) {

        // Default APPROVED — admin ko normally yahi chahiye
        ApprovalStatus effective = (approvalStatus != null) ? approvalStatus : ApprovalStatus.APPROVED;

        Page<Student> page = (status == null)
                ? studentRepository.findByApprovalStatus(effective, pageable)
                : studentRepository.findByApprovalStatusAndStatus(effective, status, pageable);

        return page.map(StudentResponse::from);
    }


    @Override
    @Transactional(readOnly = true)
    public Page<StudentResponse> getStudentsByBatch(String batchId, Pageable pageable) {

        // Batch exist karta hai? (warna khali list confuse karegi)
        batchService.getEntityOrThrow(batchId);

        return studentBatchRepository.findActiveByBatchId(batchId, pageable)
                .map(sb -> StudentResponse.from(sb.getStudent()));
    }


    @Override
    @Transactional(readOnly = true)
    public Page<StudentResponse> searchStudents(String query, Pageable pageable) {

        String q = (query == null || query.isBlank()) ? null : query.trim();

        return studentRepository.searchStudents(q, pageable)
                .map(StudentResponse::from);
    }


    // ========================================================================
    // UPDATE STATUS
    // ========================================================================
    @Override
    public StudentResponse updateStatus(String studentId, StudentStatus status) {

        Student student = getEntityOrThrow(studentId);
        student.setStatus(status);

        Student saved = studentRepository.save(student);

        log.info("Student status updated | studentId={} | status={}", studentId, status);

        return StudentResponse.from(saved);
    }


    // ========================================================================
    // INTERNAL
    // ========================================================================
    @Override
    @Transactional(readOnly = true)
    public Student getEntityOrThrow(String studentId) {

        return studentRepository.findByStudentId(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", "studentId", studentId));
    }


    // ========================================================================
    // PRIVATE HELPERS
    // ========================================================================

    /**
     * Student entity banata hai (approvalStatus set nahi karta —
     * wo calling method apne hisaab se karega).
     */
    private Student buildStudent(StudentCreateRequest request, Trainer addedBy) {

        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);

        if (studentRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Student", "email", email);
        }

        String lastId = studentRepository.findLastStudentId().orElse(null);
        String studentId = IdGeneratorUtil.nextStudentId(lastId);

        return Student.builder()
                .studentId(studentId)
                .name(request.getName().trim())
                .email(email)
                .phone(trim(request.getPhone()))
                .course(trim(request.getCourse()))
                .status(StudentStatus.ACTIVE)
                .addedByTrainer(addedBy)
                .build();
    }

    /**
     * Enrollment banata hai.
     *
     * PURANI ROW KA DHYAN
     * -----------------------------------------------------------------------
     * Student ne pehle batch chhoda tha (active = false), ab wapas
     * aa raha hai — to nayi row banane par unique constraint tootegi.
     *
     * Isliye pehle purani row dhundhte hain aur usi ko
     * dobara active kar dete hain.
     *
     * Ye method ApprovalServiceImpl bhi use karegi,
     * isliye package-private (public nahi) rakh sakte the —
     * lekin abhi private hi theek hai, wahan alag logic likha hai.
     */
    private void createEnrollment(Student student, Batch batch) {

        var existing = studentBatchRepository.findByStudentAndBatch(student, batch);

        if (existing.isPresent()) {
            StudentBatch sb = existing.get();

            if (Boolean.TRUE.equals(sb.getActive())) {
                throw new BadRequestException(
                        "Student pehle se iss batch me hai : " + student.getStudentId());
            }

            // Wapas join kar raha hai
            sb.setActive(true);
            sb.setLeftAt(null);
            sb.setJoinedAt(LocalDateTime.now());
            studentBatchRepository.save(sb);
            return;
        }

        StudentBatch enrollment = StudentBatch.builder()
                .student(student)
                .batch(batch)
                .joinedAt(LocalDateTime.now())
                .active(true)
                .build();

        studentBatchRepository.save(enrollment);
    }

    /** Student + uske batches ka full response */
    private StudentResponse buildFullResponse(Student student) {

        List<StudentBatch> enrollments =
                studentBatchRepository.findActiveByStudentId(student.getStudentId());

        return StudentResponse.from(student, enrollments, enrollments.size());
    }

    private void ensureBatchApproved(Batch batch) {
        if (batch.getApprovalStatus() != ApprovalStatus.APPROVED) {
            throw new BadRequestException(
                    "Batch abhi " + batch.getApprovalStatus() + " hai. Ismein student add nahi kar sakte.");
        }
    }

    private void ensureStudentApproved(Student student) {
        if (student.getApprovalStatus() != ApprovalStatus.APPROVED) {
            throw new BadRequestException(
                    "Student abhi " + student.getApprovalStatus() + " hai. Approve hone ke baad enroll karein.");
        }
    }

    private void ensureTrainerActive(Trainer trainer) {
        if (Boolean.FALSE.equals(trainer.getActive())) {
            throw new BadRequestException("Aapka account inactive hai");
        }
    }

    private void notifyAdmins(String trainerName, String requestType, String itemName) {

        List<String> adminEmails = adminRepository.findAllActiveEmails();

        if (adminEmails.isEmpty()) {
            log.warn("Koi active admin nahi — request ka mail nahi gaya");
            return;
        }

        adminEmails.forEach(email ->
                emailService.notifyAdminNewRequest(email, trainerName, requestType, itemName));
    }

    private String trim(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}