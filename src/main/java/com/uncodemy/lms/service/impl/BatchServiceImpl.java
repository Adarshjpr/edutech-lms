package com.uncodemy.lms.service.impl;

import com.uncodemy.lms.dto.request.BatchCreateRequest;
import com.uncodemy.lms.dto.response.BatchResponse;
import com.uncodemy.lms.exception.BadRequestException;
import com.uncodemy.lms.exception.ResourceNotFoundException;
import com.uncodemy.lms.model.Admin;
import com.uncodemy.lms.model.ApprovalRequest;
import com.uncodemy.lms.model.Batch;
import com.uncodemy.lms.model.Trainer;
import com.uncodemy.lms.model.enums.ApprovalStatus;
import com.uncodemy.lms.model.enums.BatchStatus;
import com.uncodemy.lms.model.enums.RequestType;
import com.uncodemy.lms.repository.AdminRepository;
import com.uncodemy.lms.repository.ApprovalRequestRepository;
import com.uncodemy.lms.repository.BatchRepository;
import com.uncodemy.lms.service.rule.BatchService;
import com.uncodemy.lms.service.rule.EmailService;
import com.uncodemy.lms.service.rule.TrainerService;
import com.uncodemy.lms.util.IdGeneratorUtil;
import com.uncodemy.lms.repository.StudentBatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ============================================================================
 * BatchServiceImpl   ---  API 3 + 4 + 6 + 10(base)
 * ============================================================================
 *
 * Iss file ka sabse important hissa:
 *
 *   createByAdmin()    -> seedha APPROVED
 *   createByTrainer()  -> PENDING + ApprovalRequest + admins ko mail
 *
 * Dono me 80% code same hai (ID banana, entity build karna),
 * isliye common hissa "buildBatch()" private method me hai.
 * ============================================================================
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class BatchServiceImpl implements BatchService {

    private final BatchRepository batchRepository;
    private final AdminRepository adminRepository;
    private final ApprovalRequestRepository approvalRepository;
    private final TrainerService trainerService;
    private final EmailService emailService;
private final StudentBatchRepository studentBatchRepository;
    /*
     * NOTE : StudentBatchRepository yahan nahi hai.
     * Wo Phase 4 me banegi. Tab tak "totalStudents"
     * response me null jayega. Phase 4 me inject karke bhar denge.
     */


    // ========================================================================
    // API 3  ---  ADMIN BATCH CREATE
    // ========================================================================
    @Override
    public BatchResponse createByAdmin(BatchCreateRequest request, String adminId) {

        log.info("Admin batch create | adminId={} | batchName={}", adminId, request.getBatchName());

        // --------------------------------------------------------------
        // STEP 1 : Admin valid hai?
        // --------------------------------------------------------------
        Admin admin = adminRepository.findByAdminId(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin", "adminId", adminId));

        // --------------------------------------------------------------
        // STEP 2 : Trainer assign karna hai to wo valid hai?
        // --------------------------------------------------------------
        /*
         * Admin ke liye trainerId OPTIONAL hai.
         *
         * Kyun? Kyunki admin pehle batch bana sakta hai aur
         * baad me trainer assign kar sakta hai.
         *
         * Diya hai to valid hona chahiye.
         */
        Trainer trainer = null;
        if (request.getTrainerId() != null && !request.getTrainerId().isBlank()) {
            trainer = trainerService.getEntityOrThrow(request.getTrainerId().trim());

            if (Boolean.FALSE.equals(trainer.getActive())) {
                throw new BadRequestException(
                        "Trainer inactive hai, batch assign nahi kar sakte : " + trainer.getTrainerId());
            }
        }

        // --------------------------------------------------------------
        // STEP 3 : Batch banao  --- APPROVED
        // --------------------------------------------------------------
        Batch batch = buildBatch(request, trainer);

        batch.setApprovalStatus(ApprovalStatus.APPROVED);   // admin = seedha approved
        batch.setCreatedByAdmin(admin);

        Batch saved = batchRepository.save(batch);

        log.info("Batch created by admin | batchId={} | status=APPROVED", saved.getBatchId());

        return BatchResponse.from(saved, 0);
    }


    // ========================================================================
    // API 4  ---  TRAINER BATCH CREATE  (approval flow)
    // ========================================================================
    @Override
    public BatchResponse createByTrainer(BatchCreateRequest request, String trainerId, String note) {

        log.info("Trainer batch create | trainerId={} | batchName={}", trainerId, request.getBatchName());

        // --------------------------------------------------------------
        // STEP 1 : Trainer valid hai?
        // --------------------------------------------------------------
        Trainer trainer = trainerService.getEntityOrThrow(trainerId);

        if (Boolean.FALSE.equals(trainer.getActive())) {
            throw new BadRequestException("Aapka account inactive hai, batch nahi bana sakte");
        }

        // --------------------------------------------------------------
        // STEP 2 : Batch banao  --- PENDING
        // --------------------------------------------------------------
        /*
         * DHYAN DO : request.getTrainerId() IGNORE kar rahe hain.
         *
         * Trainer khud hi apne batch ka trainer hoga.
         * Warna trainer A, trainer B ke naam se batch bana deta.
         */
        Batch batch = buildBatch(request, trainer);

        batch.setApprovalStatus(ApprovalStatus.PENDING);    // admin approve karega
        batch.setCreatedByTrainer(trainer);

        Batch saved = batchRepository.save(batch);

        // --------------------------------------------------------------
        // STEP 3 : Approval Request banao
        // --------------------------------------------------------------
        ApprovalRequest approvalRequest = ApprovalRequest.builder()
                .requestType(RequestType.BATCH_CREATE)
                .status(ApprovalStatus.PENDING)
                .requestedByTrainer(trainer)
                .batch(saved)
                .requestNote(note != null && !note.isBlank() ? note.trim() : null)
                .build();

        approvalRepository.save(approvalRequest);

        log.info("Batch created by trainer | batchId={} | status=PENDING | requestId={}",
                saved.getBatchId(), approvalRequest.getId());

        // --------------------------------------------------------------
        // STEP 4 : Saare admins ko mail
        // --------------------------------------------------------------
        /*
         * SAARE admins ko kyun?
         *
         * Kyunki ek admin chhutti pe ho sakta hai. Jo pehle
         * dekhega wo approve kar dega — request atki nahi rahegi.
         *
         * Mail async hai, isliye ye loop turant khatam ho jayega.
         */
        notifyAdmins(trainer.getName(), "Batch Creation", saved.getBatchName());

        return BatchResponse.from(saved, 0);
    }


    // ========================================================================
    // API 6  ---  CURRENT TOPIC UPDATE
    // ========================================================================
    @Override
    public BatchResponse updateCurrentTopic(String batchId, String topic, String updatedByTrainerId) {

        // --------------------------------------------------------------
        // STEP 1 : Trainer kar raha hai to batch uska hai?
        // --------------------------------------------------------------
        /*
         * updatedByTrainerId null hai  -> ADMIN kar raha hai -> koi check nahi
         * value hai                    -> TRAINER kar raha hai -> ownership check
         */
        if (updatedByTrainerId != null && !updatedByTrainerId.isBlank()) {
            verifyBatchBelongsToTrainer(batchId, updatedByTrainerId);
        }

        Batch batch = getEntityOrThrow(batchId);

        // --------------------------------------------------------------
        // STEP 2 : PENDING batch me topic set nahi kar sakte
        // --------------------------------------------------------------
        /*
         * Jo batch abhi approve hi nahi hua, uska topic
         * set karne ka koi matlab nahi.
         *
         * Aur API 10 ki search me PENDING batch ka topic
         * aa jata to confusion hota.
         */
        if (batch.getApprovalStatus() != ApprovalStatus.APPROVED) {
            throw new BadRequestException(
                    "Ye batch abhi " + batch.getApprovalStatus() + " hai. Approve hone ke baad hi topic set kar sakte hain.");
        }

        if (topic == null || topic.isBlank()) {
            throw new BadRequestException("Topic khali nahi ho sakta");
        }

        // --------------------------------------------------------------
        // STEP 3 : Update
        // --------------------------------------------------------------
        batch.setCurrentTopic(topic.trim());

        /*
         * topicUpdatedAt bhi set kar rahe hain.
         *
         * API 10 me kaam aayega — admin dekh sakega ki
         * kis batch ka topic 15 din se update nahi hua
         * (matlab trainer update hi nahi kar raha).
         */
        batch.setTopicUpdatedAt(LocalDateTime.now());

        Batch saved = batchRepository.save(batch);

        log.info("Current topic updated | batchId={} | topic={} | by={}",
                batchId, topic, updatedByTrainerId != null ? updatedByTrainerId : "ADMIN");

        return BatchResponse.from(saved);
    }


    // ========================================================================
    // READ
    // ========================================================================
@Override
    @Transactional(readOnly = true)
    public BatchResponse getByBatchId(String batchId) {

        Batch batch = batchRepository.findByBatchIdWithTrainer(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Batch", "batchId", batchId));

        // Ab actual count aayega
        int totalStudents = (int) studentBatchRepository.countActiveByBatchId(batchId);

        return BatchResponse.from(batch, totalStudents);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BatchResponse> getAllApprovedBatches(BatchStatus status, Pageable pageable) {

        Page<Batch> page = (status == null)
                ? batchRepository.findByApprovalStatus(ApprovalStatus.APPROVED, pageable)
                : batchRepository.findByApprovalStatusAndStatus(ApprovalStatus.APPROVED, status, pageable);

        return page.map(BatchResponse::from);
    }


    @Override
    @Transactional(readOnly = true)
    public Page<BatchResponse> getBatchesByTrainer(String trainerId, boolean onlyApproved, Pageable pageable) {

        Trainer trainer = trainerService.getEntityOrThrow(trainerId);

        Page<Batch> page = onlyApproved
                ? batchRepository.findByTrainerAndApprovalStatus(trainer, ApprovalStatus.APPROVED, pageable)
                : batchRepository.findByTrainer(trainer, pageable);

        return page.map(BatchResponse::from);
    }


    // ========================================================================
    // API 10  ---  SEARCH (base)
    // ========================================================================
    @Override
    @Transactional(readOnly = true)
    public Page<BatchResponse> searchBatches(String trainerName, String topic, Pageable pageable) {

        /*
         * Khali string ko null bana rahe hain.
         *
         * Kyun? Kyunki repository ki query me
         * ":param IS NULL OR ..." ka logic hai.
         *
         * Agar "" (khali string) gaya to wo NULL nahi hai —
         * matlab filter lag jayega aur LIKE '%%' se sab kuch
         * match karega... jo waise to theek hai, lekin
         * null bhejna saaf aur fast hai.
         */
        String name  = blankToNull(trainerName);
        String tpc   = blankToNull(topic);

        return batchRepository.searchBatches(name, tpc, pageable)
                .map(BatchResponse::from);
    }


    // ========================================================================
    // UPDATE STATUS
    // ========================================================================
    @Override
    public BatchResponse updateStatus(String batchId, BatchStatus status) {

        Batch batch = getEntityOrThrow(batchId);

        if (batch.getApprovalStatus() != ApprovalStatus.APPROVED) {
            throw new BadRequestException("Sirf approved batch ka status badal sakte hain");
        }

        batch.setStatus(status);
        Batch saved = batchRepository.save(batch);

        log.info("Batch status updated | batchId={} | status={}", batchId, status);

        return BatchResponse.from(saved);
    }


    // ========================================================================
    // INTERNAL
    // ========================================================================
    @Override
    @Transactional(readOnly = true)
    public Batch getEntityOrThrow(String batchId) {

        return batchRepository.findByBatchId(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Batch", "batchId", batchId));
    }


    @Override
    @Transactional(readOnly = true)
    public void verifyBatchBelongsToTrainer(String batchId, String trainerId) {

        Trainer trainer = trainerService.getEntityOrThrow(trainerId);

        boolean owns = batchRepository.existsByBatchIdAndTrainer(batchId, trainer);

        if (!owns) {
            /*
             * SOCH KE 404 NAHI, 400 DIYA HAI.
             *
             * "Batch not found" bolte to trainer ko pata chal jata
             * ki batch exist to karta hai (kyunki galat ID pe bhi
             * yahi message aata).
             *
             * Ye message saaf batata hai ki permission ka issue hai.
             *
             * Security aane par ye 403 FORBIDDEN ban jayega.
             */
            throw new BadRequestException(
                    "Ye batch aapka nahi hai. batchId : " + batchId);
        }
    }


    // ========================================================================
    // PRIVATE HELPERS
    // ========================================================================

    /**
     * Admin aur Trainer dono ke create me jo COMMON hai.
     *
     * approvalStatus aur createdBy yahan SET NAHI hote —
     * wo calling method apne hisaab se set karta hai.
     */
    private Batch buildBatch(BatchCreateRequest request, Trainer trainer) {

        String batchName = request.getBatchName().trim();

        // --------------------------------------------------------------
        // Batch ID banao :  "Java Full Stack" -> JAVA101
        // --------------------------------------------------------------
        /*
         * Do step me hota hai:
         *   1. naam se prefix nikalo         -> "JAVA"
         *   2. uss prefix ki last ID dhundo  -> "JAVA103"
         *   3. agli banao                    -> "JAVA104"
         *
         * Har prefix ka apna counter chalta hai.
         */
        String prefix  = IdGeneratorUtil.batchPrefix(batchName);
        String lastId  = batchRepository.findLastBatchIdByPrefix(prefix).orElse(null);
        String batchId = IdGeneratorUtil.nextBatchId(batchName, lastId);

        log.debug("Generated batchId={} (prefix={}, last={})", batchId, prefix, lastId);

        return Batch.builder()
                .batchId(batchId)
                .batchName(batchName)
                .timing(trim(request.getTiming()))
                .meetLink(trim(request.getMeetLink()))
                .communityLink(trim(request.getCommunityLink()))
                .certificateLink(trim(request.getCertificateLink()))
                .currentTopic(trim(request.getCurrentTopic()))
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())

                // status na bheja to UPCOMING maan lo
                .status(request.getStatus() != null ? request.getStatus() : BatchStatus.UPCOMING)

                .trainer(trainer)

                // topic diya hai to timestamp bhi set kar do
                .topicUpdatedAt(
                        (request.getCurrentTopic() != null && !request.getCurrentTopic().isBlank())
                                ? LocalDateTime.now() : null)
                .build();
    }


    /**
     * Saare active admins ko nayi request ka mail.
     */
    private void notifyAdmins(String trainerName, String requestType, String itemName) {

        List<String> adminEmails = adminRepository.findAllActiveEmails();

        if (adminEmails.isEmpty()) {
            log.warn("Koi active admin nahi mila — approval request ka mail nahi gaya");
            return;
        }

        for (String adminEmail : adminEmails) {
            emailService.notifyAdminNewRequest(adminEmail, trainerName, requestType, itemName);
        }

        log.info("Approval request mail bheji | admins={}", adminEmails.size());
    }


    private String trim(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}