package com.uncodemy.lms.repository;

import com.uncodemy.lms.model.ApprovalRequest;
import com.uncodemy.lms.model.Batch;
import com.uncodemy.lms.model.Student;
import com.uncodemy.lms.model.Trainer;
import com.uncodemy.lms.model.enums.ApprovalStatus;
import com.uncodemy.lms.model.enums.RequestType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * ============================================================================
 * ApprovalRequestRepository
 * ============================================================================
 *
 * Trainer ki bheji hui approval requests ki queries.
 *
 * Kahan use hoga:
 *   API 4 -> Trainer batch banaye  -> BATCH_CREATE request
 *   API 7 -> Trainer student add kare -> STUDENT_ADD request
 *
 * DO TARAF SE DEKHA JAYEGA
 * ---------------------------------------------------------------------------
 *
 *   ADMIN ki taraf se :
 *     "Mere paas kitni pending requests hain?"
 *     -> findByStatus(PENDING)
 *
 *   TRAINER ki taraf se :
 *     "Meri request ka kya hua?"
 *     -> findByRequestedByTrainer(trainer)
 *
 * Dono ke liye alag-alag methods neeche hain.
 * ============================================================================
 */
@Repository
public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, Long> {

    // ========================================================================
    // 1. ADMIN KA DASHBOARD  --- pending requests
    // ========================================================================

    /**
     * Saari requests status ke hisaab se.
     *
     * Admin dashboard ki main list:
     *   findByStatus(PENDING, pageable)  -> jo approve karni hain
     *   findByStatus(APPROVED, pageable) -> history
     */
    Page<ApprovalRequest> findByStatus(ApprovalStatus status, Pageable pageable);

    /**
     * Status + type dono ka filter.
     *
     * Example: sirf pending BATCH_CREATE requests dekhni hain.
     */
    Page<ApprovalRequest> findByStatusAndRequestType(
            ApprovalStatus status, RequestType requestType, Pageable pageable);

    /**
     * Pending requests — POORI DETAILS ke saath, EK HI QUERY me.
     *
     * JOIN FETCH KYUN LAGAYA?
     * -----------------------------------------------------------------------
     * ApprovalRequest me trainer, batch, student — teeno LAZY hain.
     *
     * Admin dashboard pe 20 requests dikhani hain, aur har ek me
     * trainer ka naam + batch ka naam chahiye.
     *
     * Bina JOIN FETCH ke:
     *   1 query  -> 20 requests
     *   20 query -> har request ka trainer
     *   20 query -> har request ka batch
     *   ---------------------------------
     *   41 QUERY!  (classic N+1 problem)
     *
     * JOIN FETCH ke saath: 1 QUERY. Bas.
     *
     * LEFT JOIN kyun, normal JOIN kyun nahi?
     * -----------------------------------------------------------------------
     * Kyunki BATCH_CREATE request me "student" NULL hota hai.
     *
     * Normal JOIN us row ko RESULT SE HI HATA DETA (kyunki
     * join karne ko kuch mila hi nahi).
     *
     * LEFT JOIN null wali rows bhi rakhta hai.
     *
     * Matlab normal JOIN lagate to admin ko batch requests
     * dikhti hi nahi — sirf student wali dikhti. Bada bug hota.
     */
    @Query(value = """
            SELECT ar FROM ApprovalRequest ar
            LEFT JOIN FETCH ar.requestedByTrainer
            LEFT JOIN FETCH ar.batch
            LEFT JOIN FETCH ar.student
            WHERE ar.status = :status
            """,
            countQuery = """
            SELECT COUNT(ar) FROM ApprovalRequest ar
            WHERE ar.status = :status
            """)
    Page<ApprovalRequest> findByStatusWithDetails(@Param("status") ApprovalStatus status,
                                                  Pageable pageable);

    /**
     * Ek request — poori details ke saath.
     *
     * Approve/reject karte waqt ye use hogi, kyunki
     * mail bhejne ke liye trainer ka email aur naam chahiye.
     */
    @Query("""
           SELECT ar FROM ApprovalRequest ar
           LEFT JOIN FETCH ar.requestedByTrainer
           LEFT JOIN FETCH ar.batch
           LEFT JOIN FETCH ar.student
           WHERE ar.id = :id
           """)
    Optional<ApprovalRequest> findByIdWithDetails(@Param("id") Long id);


    // ========================================================================
    // 2. TRAINER KA DASHBOARD  --- meri requests
    // ========================================================================

    /**
     * Trainer ki saari requests (har status).
     *
     * "Maine kya-kya bheja aur kya hua" — trainer ko
     * ye dikhna zaroori hai.
     */
    Page<ApprovalRequest> findByRequestedByTrainer(Trainer trainer, Pageable pageable);

    /**
     * Trainer ki requests, status ke hisaab se.
     *
     * Example: "meri kaunsi requests abhi pending hain"
     */
    Page<ApprovalRequest> findByRequestedByTrainerAndStatus(
            Trainer trainer, ApprovalStatus status, Pageable pageable);


    // ========================================================================
    // 3. DUPLICATE REQUEST ROKNA
    // ========================================================================

    /**
     * Iss batch ki koi PENDING request pehle se hai?
     *
     * KYUN ZAROORI HAI
     * -----------------------------------------------------------------------
     * Trainer ne request bheji, admin ne abhi dekhi nahi.
     * Trainer ko lagta hai kuch hua hi nahi, wo dobara
     * bhej deta hai.
     *
     * Ab admin ke paas SAME batch ki 2 requests hain.
     * Ek approve ki, doosri approve karne jaye to
     * "already approved" error.
     *
     * Isse pehle hi rok denge.
     */
    boolean existsByBatchAndStatus(Batch batch, ApprovalStatus status);

    /**
     * Iss student ki koi PENDING request pehle se hai?
     * (API 7 ke liye)
     */
    boolean existsByStudentAndStatus(Student student, ApprovalStatus status);


    // ========================================================================
    // 4. TARGET SE REQUEST DHUNDO
    // ========================================================================

    /**
     * Kisi batch ki pending request nikalo.
     *
     * KAB CHAHIYE
     * -----------------------------------------------------------------------
     * Admin do jagah se approve kar sakta hai:
     *
     *   1. Approval dashboard se  -> requestId pata hai ✔
     *   2. Batch list se seedha    -> sirf batchId pata hai
     *
     * Doosre case me batchId se request dhundhni padegi,
     * taaki uska status bhi update ho jaye.
     *
     * Warna batch to APPROVED ho jayega lekin request
     * PENDING hi padi rahegi — admin ke dashboard me
     * hamesha dikhti rahegi. Gandi cheez.
     */
    Optional<ApprovalRequest> findByBatchAndStatus(Batch batch, ApprovalStatus status);

    /**
     * Kisi student ki pending request nikalo.
     */
    Optional<ApprovalRequest> findByStudentAndStatus(Student student, ApprovalStatus status);


    // ========================================================================
    // 5. COUNT  --- badge / notification ke liye
    // ========================================================================

    /**
     * Kitni requests pending hain.
     *
     * Admin dashboard pe laal badge dikhane ke liye:
     *   "Approvals (3)"
     */
    long countByStatus(ApprovalStatus status);

    /**
     * Type ke hisaab se count.
     *
     * "Batch Requests (2) | Student Requests (5)"
     */
    long countByStatusAndRequestType(ApprovalStatus status, RequestType requestType);

    /**
     * Ek trainer ki kitni requests pending hain.
     *
     * Trainer ke apne dashboard pe dikhane ke liye.
     */
    long countByRequestedByTrainerAndStatus(Trainer trainer, ApprovalStatus status);
}