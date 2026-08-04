package com.uncodemy.lms.service.impl;

import com.uncodemy.lms.dto.request.ApprovalActionRequest;
import com.uncodemy.lms.dto.response.ApprovalRequestResponse;
import com.uncodemy.lms.exception.BadRequestException;
import com.uncodemy.lms.exception.ResourceNotFoundException;
import com.uncodemy.lms.model.*;
import com.uncodemy.lms.model.enums.ApprovalStatus;
import com.uncodemy.lms.model.enums.RequestType;
import com.uncodemy.lms.repository.AdminRepository;
import com.uncodemy.lms.repository.ApprovalRequestRepository;
import com.uncodemy.lms.repository.BatchRepository;
import com.uncodemy.lms.service.rule.ApprovalService;
import com.uncodemy.lms.service.rule.EmailService;
import com.uncodemy.lms.service.rule.TrainerService;
import com.uncodemy.lms.repository.StudentRepository;
import com.uncodemy.lms.repository.StudentBatchRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * ============================================================================
 * ApprovalServiceImpl
 * ============================================================================
 *
 * Admin ke approve / reject ka poora logic.
 *
 * SABSE ZAROORI CHECK : "pehle se review to nahi ho chuki?"
 * ---------------------------------------------------------------------------
 * Do admin ek saath dashboard khole hue hain. Dono ne same
 * request pe approve daba diya.
 *
 * Bina check ke:
 *   - request 2 baar approve hogi
 *   - trainer ko 2 mail jayengi
 *   - reviewedBy me doosre admin ka naam chadh jayega
 *
 * Isliye har action se pehle status check hota hai.
 * ============================================================================
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ApprovalServiceImpl implements ApprovalService {

    private final ApprovalRequestRepository approvalRepository;
    private final AdminRepository adminRepository;
    private final BatchRepository batchRepository;
    private final TrainerService trainerService;
    private final EmailService emailService;
// class ke andar, baaki fields ke saath
private final StudentRepository studentRepository;
private final StudentBatchRepository studentBatchRepository;
    /*
     * NOTE : StudentRepository aur StudentBatchRepository
     * yahan nahi hain — wo Phase 4 me banengi.
     *
     * Isliye abhi STUDENT_ADD approve karne pe sirf
     * request ka status badlega. Poora enrollment
     * Phase 4 me judega (neeche TODO marked hai).
     */


    // ========================================================================
    // APPROVE
    // ========================================================================
    @Override
    public ApprovalRequestResponse approve(Long requestId, ApprovalActionRequest action) {

        log.info("Approve request | requestId={} | adminId={}", requestId, action.getAdminId());

        // --------------------------------------------------------------
        // STEP 1 : Request nikalo aur validate karo
        // --------------------------------------------------------------
        ApprovalRequest request = approvalRepository.findByIdWithDetails(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("ApprovalRequest", "id", requestId));

        ensurePending(request);

        Admin admin = getAdminOrThrow(action.getAdminId());

        // --------------------------------------------------------------
        // STEP 2 : Request ka status update
        // --------------------------------------------------------------
        request.setStatus(ApprovalStatus.APPROVED);
        request.setReviewedByAdmin(admin);
        request.setReviewedAt(LocalDateTime.now());
        request.setAdminRemark(trim(action.getRemark()));

        // --------------------------------------------------------------
        // STEP 3 : Type ke hisaab se asli kaam
        // --------------------------------------------------------------
        String itemName;

        if (request.getRequestType() == RequestType.BATCH_CREATE) {
            itemName = approveBatch(request, admin);

        } else if (request.getRequestType() == RequestType.STUDENT_ADD) {
            itemName = approveStudent(request);

        } else {
            throw new BadRequestException("Unknown request type : " + request.getRequestType());
        }

        approvalRepository.save(request);

        // --------------------------------------------------------------
        // STEP 4 : Trainer ko mail
        // --------------------------------------------------------------
        Trainer trainer = request.getRequestedByTrainer();

        emailService.sendApprovalAccepted(
                trainer.getEmail(),
                trainer.getName(),
                readableType(request.getRequestType()),
                itemName
        );

        log.info("Request APPROVED | requestId={} | type={} | item={}",
                requestId, request.getRequestType(), itemName);

        return ApprovalRequestResponse.from(request);
    }


    // ========================================================================
    // REJECT
    // ========================================================================
    @Override
    public ApprovalRequestResponse reject(Long requestId, ApprovalActionRequest action) {

        log.info("Reject request | requestId={} | adminId={}", requestId, action.getAdminId());

        // --------------------------------------------------------------
        // STEP 1 : Reason ZAROORI hai
        // --------------------------------------------------------------
        /*
         * Ye check DTO me @NotBlank se nahi ho sakta,
         * kyunki approve me remark optional hai.
         *
         * Isliye yahan manually.
         *
         * Kyun zaroori hai? Trainer ne mehnat se batch banaya,
         * reject ho gaya, aur pata hi nahi kyun — ye frustrating hai.
         */
        if (action.getRemark() == null || action.getRemark().isBlank()) {
            throw new BadRequestException(
                    "Reject karne ke liye reason (remark) dena zaroori hai");
        }

        ApprovalRequest request = approvalRepository.findByIdWithDetails(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("ApprovalRequest", "id", requestId));

        ensurePending(request);

        Admin admin = getAdminOrThrow(action.getAdminId());

        // --------------------------------------------------------------
        // STEP 2 : Request update
        // --------------------------------------------------------------
        request.setStatus(ApprovalStatus.REJECTED);
        request.setReviewedByAdmin(admin);
        request.setReviewedAt(LocalDateTime.now());
        request.setAdminRemark(action.getRemark().trim());

        // --------------------------------------------------------------
        // STEP 3 : Target ka status bhi REJECTED
        // --------------------------------------------------------------
        String itemName = "N/A";

        if (request.getRequestType() == RequestType.BATCH_CREATE && request.getBatch() != null) {

            Batch batch = request.getBatch();
            batch.setApprovalStatus(ApprovalStatus.REJECTED);
            batchRepository.save(batch);

            itemName = batch.getBatchName() + " (" + batch.getBatchId() + ")";

            /*
             * Batch ki row DELETE nahi kar rahe.
             *
             * Kyun? Kyunki:
             *   - History rehni chahiye (kya-kya reject hua)
             *   - Trainer dekh sake ki uska kya reject hua tha
             *   - Delete karne pe approval request ka FK toot jata
             *
             * REJECTED status wale batch kisi list me nahi aate,
             * kyunki har query me APPROVED ka filter laga hai.
             */

        } else if (request.getRequestType() == RequestType.STUDENT_ADD && request.getStudent() != null) {

            Student student = request.getStudent();
            student.setApprovalStatus(ApprovalStatus.REJECTED);
            // studentRepository.save() Phase 4 me — abhi dirty checking se save ho jayega
studentRepository.save(student);
            itemName = student.getName() + " (" + student.getStudentId() + ")";
        }

        approvalRepository.save(request);

        // --------------------------------------------------------------
        // STEP 4 : Trainer ko mail --- REASON ke saath
        // --------------------------------------------------------------
        Trainer trainer = request.getRequestedByTrainer();

        emailService.sendApprovalRejected(
                trainer.getEmail(),
                trainer.getName(),
                readableType(request.getRequestType()),
                itemName,
                request.getAdminRemark()
        );

        log.info("Request REJECTED | requestId={} | reason={}", requestId, request.getAdminRemark());

        return ApprovalRequestResponse.from(request);
    }


    // ========================================================================
    // READ  ---  admin dashboard
    // ========================================================================
    @Override
    @Transactional(readOnly = true)
    public Page<ApprovalRequestResponse> getRequests(ApprovalStatus status,
                                                     RequestType requestType,
                                                     Pageable pageable) {

        // Status na diya to PENDING default (admin ko yahi chahiye hota hai)
        ApprovalStatus effectiveStatus = (status != null) ? status : ApprovalStatus.PENDING;

        Page<ApprovalRequest> page;

        if (requestType != null) {
            page = approvalRepository.findByStatusAndRequestType(effectiveStatus, requestType, pageable);
        } else {
            // JOIN FETCH wali — N+1 se bachne ke liye
            page = approvalRepository.findByStatusWithDetails(effectiveStatus, pageable);
        }

        return page.map(ApprovalRequestResponse::from);
    }


    @Override
    @Transactional(readOnly = true)
    public ApprovalRequestResponse getById(Long requestId) {

        ApprovalRequest request = approvalRepository.findByIdWithDetails(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("ApprovalRequest", "id", requestId));

        return ApprovalRequestResponse.from(request);
    }


    @Override
    @Transactional(readOnly = true)
    public long countPending() {
        return approvalRepository.countByStatus(ApprovalStatus.PENDING);
    }


    // ========================================================================
    // READ  ---  trainer dashboard
    // ========================================================================
    @Override
    @Transactional(readOnly = true)
    public Page<ApprovalRequestResponse> getMyRequests(String trainerId,
                                                       ApprovalStatus status,
                                                       Pageable pageable) {

        Trainer trainer = trainerService.getEntityOrThrow(trainerId);

        Page<ApprovalRequest> page = (status == null)
                ? approvalRepository.findByRequestedByTrainer(trainer, pageable)
                : approvalRepository.findByRequestedByTrainerAndStatus(trainer, status, pageable);

        return page.map(ApprovalRequestResponse::from);
    }


    // ========================================================================
    // PRIVATE  ---  approve ka asli kaam
    // ========================================================================

    /**
     * BATCH_CREATE approve.
     *
     * @return batch ka naam (mail me dikhane ke liye)
     */
    private String approveBatch(ApprovalRequest request, Admin admin) {

        Batch batch = request.getBatch();

        if (batch == null) {
            throw new BadRequestException(
                    "Iss request ka batch nahi mila. Data corrupt ho sakta hai. requestId : " + request.getId());
        }

        batch.setApprovalStatus(ApprovalStatus.APPROVED);

        /*
         * createdByAdmin bhi set kar rahe hain.
         *
         * Batch banaya trainer ne, lekin FINAL APPROVAL
         * admin ne diya — dono ka record rehna chahiye.
         *
         * createdByTrainer pehle se set hai (BatchServiceImpl me).
         */
        batch.setCreatedByAdmin(admin);

        batchRepository.save(batch);

        return batch.getBatchName() + " (" + batch.getBatchId() + ")";
    }


 /**
     * STUDENT_ADD approve  ---  ab COMPLETE hai
     *
     * TEEN KAAM:
     *   1. Student ko APPROVED karo
     *   2. Enrollment (StudentBatch) banao
     *   3. Student ko welcome mail
     */
    private String approveStudent(ApprovalRequest request) {

        Student student = request.getStudent();

        if (student == null) {
            throw new BadRequestException(
                    "Iss request ka student nahi mila. requestId : " + request.getId());
        }

        // ---- 1. Student approve ----
        student.setApprovalStatus(ApprovalStatus.APPROVED);
        studentRepository.save(student);

        // ---- 2. Enrollment banao ----
        Batch batch = request.getBatch();
        String batchName = "Uncodemy";

        if (batch != null) {
            batchName = batch.getBatchName();

            /*
             * Purani row check kar rahe hain.
             *
             * Do case:
             *   - Student pehle iss batch me tha, chhoda, wapas aa raha
             *     -> purani row ko active karo
             *   - Bilkul naya
             *     -> nayi row
             *
             * Bina iss check ke unique constraint tootegi.
             */
            var existing = studentBatchRepository.findByStudentAndBatch(student, batch);

            if (existing.isPresent()) {
                StudentBatch sb = existing.get();
                sb.setActive(true);
                sb.setLeftAt(null);
                sb.setJoinedAt(LocalDateTime.now());
                studentBatchRepository.save(sb);

            } else {
                StudentBatch enrollment = StudentBatch.builder()
                        .student(student)
                        .batch(batch)
                        .joinedAt(LocalDateTime.now())
                        .active(true)
                        .build();
                studentBatchRepository.save(enrollment);
            }

            log.info("Enrollment created | studentId={} | batchId={}",
                    student.getStudentId(), batch.getBatchId());
        }

        // ---- 3. Student ko welcome mail ----
        /*
         * Ab bhej rahe hain, pehle nahi.
         *
         * Kyunki trainer ke add karte waqt student PENDING tha —
         * agar tab mail chali jati aur admin reject kar deta,
         * to student confuse ho jata.
         */
        emailService.sendStudentWelcome(
                student.getEmail(),
                student.getName(),
                student.getStudentId(),
                batchName
        );

        return student.getName() + " (" + student.getStudentId() + ")"
                + (batch != null ? " -> " + batch.getBatchName() : "");
    }
    // ========================================================================
    // PRIVATE HELPERS
    // ========================================================================

    /**
     * Request abhi PENDING hai ya nahi.
     *
     * Ye check DOUBLE-APPROVE se bachata hai.
     */
    private void ensurePending(ApprovalRequest request) {

        if (request.getStatus() != ApprovalStatus.PENDING) {
            throw new BadRequestException(
                    "Ye request pehle se " + request.getStatus()
                    + " ho chuki hai. Dobara action nahi le sakte.");
        }
    }

    private Admin getAdminOrThrow(String adminId) {

        return adminRepository.findByAdminId(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin", "adminId", adminId));
    }

    /**
     * Enum ko insaan ke padhne layak banata hai (mail ke liye).
     *
     * BATCH_CREATE -> "Batch Creation"
     */
    private String readableType(RequestType type) {

        return switch (type) {
            case BATCH_CREATE -> "Batch Creation";
            case STUDENT_ADD  -> "Student Addition";
        };
    }

    private String trim(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}