package com.uncodemy.lms.service.rule;


import com.uncodemy.lms.dto.request.BatchCreateRequest;
import com.uncodemy.lms.dto.response.BatchResponse;
import com.uncodemy.lms.model.Batch;
import com.uncodemy.lms.model.enums.BatchStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * ============================================================================
 * BatchService  (Interface)
 * ============================================================================
 *
 * API 3  -> Admin batch create
 * API 4  -> Trainer batch create (approval ke saath)
 * API 6  -> Current topic update
 * API 10 -> Search (base)
 *
 * DO ALAG CREATE METHOD KYUN?
 * ---------------------------------------------------------------------------
 * Admin aur Trainer dono batch banate hain, lekin behaviour
 * bilkul alag hai:
 *
 *   ADMIN                          TRAINER
 *   -----                          -------
 *   approvalStatus = APPROVED      approvalStatus = PENDING
 *   trainer assign kar sakta hai   khud hi trainer hai
 *   koi approval nahi              ApprovalRequest banti hai
 *   koi mail nahi                  admins ko mail jati hai
 *
 * Ek hi method me "boolean isAdmin" pass karke if-else
 * likhna ganda hota — do alag method saaf hai.
 * ============================================================================
 */
public interface BatchService {

    // ========================================================================
    // API 3 --- ADMIN CREATE
    // ========================================================================

    /**
     * Admin batch banata hai — SEEDHA APPROVED.
     *
     * ANDAR KYA HOGA:
     * -----------------------------------------------------------------------
     *  1. Admin exist karta hai?           -> 404 agar nahi
     *  2. Trainer diya hai to wo valid hai? -> 404 agar nahi
     *  3. batchId banao ("Java..." -> JAVA101)
     *  4. approvalStatus = APPROVED
     *  5. createdByAdmin = ye admin
     *  6. Save
     *
     * Koi approval request nahi banegi, koi mail nahi.
     *
     * @param adminId kaunsa admin bana raha hai
     *                (security aane par ye hat jayega)
     */
    BatchResponse createByAdmin(BatchCreateRequest request, String adminId);


    // ========================================================================
    // API 4 --- TRAINER CREATE  (approval ke saath)
    // ========================================================================

    /**
     * Trainer batch banata hai — PENDING me jata hai.
     *
     * ANDAR KYA HOGA:
     * -----------------------------------------------------------------------
     *  1. Trainer valid hai?
     *  2. batchId banao
     *  3. approvalStatus = PENDING
     *  4. trainer = khud       (request ka trainerId ignore hoga)
     *  5. createdByTrainer = khud
     *  6. Save
     *  7. ApprovalRequest banao (BATCH_CREATE)
     *  8. Saare admins ko mail
     *
     * Batch DB me to ban jayega, lekin PENDING hone ki wajah se
     * kisi list me nahi dikhega jab tak approve na ho.
     *
     * @param trainerId kaun bana raha hai
     * @param note      trainer ka message admin ke liye (optional)
     */
    BatchResponse createByTrainer(BatchCreateRequest request, String trainerId, String note);


    // ========================================================================
    // API 6 --- CURRENT TOPIC UPDATE
    // ========================================================================

    /**
     * "Batch me abhi kya chal raha hai" update karta hai.
     *
     * Admin aur Trainer dono kar sakte hain (API 6 ka requirement).
     *
     * topicUpdatedAt bhi automatically set hoga —
     * API 10 ki search me kaam aayega ("kitna purana topic hai").
     *
     * @param updatedByTrainerId agar trainer kar raha hai to uski ID,
     *                           admin kar raha hai to null.
     *                           Trainer ke case me check hoga ki
     *                           batch usi ka hai ya nahi.
     */
    BatchResponse updateCurrentTopic(String batchId, String topic, String updatedByTrainerId);


    // ========================================================================
    // READ
    // ========================================================================

    /**
     * Ek batch nikalo.
     *
     * @throws com.uncodemy.lms.exception.ResourceNotFoundException
     */
    BatchResponse getByBatchId(String batchId);

    /**
     * Saare APPROVED batches.
     *
     * @param status optional filter (ACTIVE / UPCOMING / COMPLETED)
     */
    Page<BatchResponse> getAllApprovedBatches(BatchStatus status, Pageable pageable);

    /**
     * Ek trainer ke batches.
     *
     * @param onlyApproved true  = sirf approved
     *                     false = pending bhi dikhao
     *                             (trainer apne dashboard pe dekh sake
     *                              ki request ka kya hua)
     */
    Page<BatchResponse> getBatchesByTrainer(String trainerId, boolean onlyApproved, Pageable pageable);


    // ========================================================================
    // API 10 --- SEARCH  (base version)
    // ========================================================================

    /**
     * Trainer ke naam ya current topic se batch dhundo.
     *
     * Dono parameter OPTIONAL hain:
     *   sirf naam    -> uss trainer ke batches
     *   sirf topic   -> jahan wo topic chal raha
     *   dono         -> dono ka match
     *   kuch nahi    -> saare approved batches
     *
     * NOTE: Phase 8 me iska full version banega.
     */
    Page<BatchResponse> searchBatches(String trainerName, String topic, Pageable pageable);


    // ========================================================================
    // UPDATE  --- batch status
    // ========================================================================

    /**
     * Batch ka status badlo (UPCOMING -> ACTIVE -> COMPLETED).
     */
    BatchResponse updateStatus(String batchId, BatchStatus status);


    // ========================================================================
    // INTERNAL  --- doosri services ke liye
    // ========================================================================

    /**
     * Batch ki ENTITY nikalta hai.
     *
     * StudentService, AnnouncementService, ContentService —
     * sabko batch entity chahiye hogi relation set karne ke liye.
     *
     * @throws com.uncodemy.lms.exception.ResourceNotFoundException
     */
    Batch getEntityOrThrow(String batchId);

    /**
     * SECURITY CHECK --- ye batch iss trainer ka hai?
     *
     * KYUN ZAROORI HAI
     * -----------------------------------------------------------------------
     * Abhi login nahi hai, to trainer request me KOI BHI
     * batchId bhej sakta hai.
     *
     * Bina iss check ke Trainer A, Trainer B ke batch me:
     *   - announcement bhej sakta hai
     *   - content upload kar sakta hai
     *   - student add kar sakta hai
     *   - topic badal sakta hai
     *
     * Isliye jahan bhi trainer batch pe kuch kare,
     * pehle ye check chalega.
     *
     * @throws com.uncodemy.lms.exception.BadRequestException agar batch uska nahi
     */
    void verifyBatchBelongsToTrainer(String batchId, String trainerId);
}