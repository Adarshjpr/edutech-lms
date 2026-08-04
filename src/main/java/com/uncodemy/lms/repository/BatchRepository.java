package com.uncodemy.lms.repository;

import com.uncodemy.lms.model.Batch;
import com.uncodemy.lms.model.Trainer;
import com.uncodemy.lms.model.enums.ApprovalStatus;
import com.uncodemy.lms.model.enums.BatchStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * ============================================================================
 * BatchRepository
 * ============================================================================
 *
 * Batch table ki saari queries.
 *
 * Ye repository in APIs me kaam aayegi:
 *   API 3  -> Admin batch create
 *   API 4  -> Trainer batch create + approval
 *   API 5  -> Batch me student add
 *   API 6  -> Current topic update
 *   API 10 -> Trainer name / topic se search
 *
 * SABSE ZAROORI BAAT : approvalStatus KA FILTER
 * ---------------------------------------------------------------------------
 * Phase 0 me humne decide kiya tha ki trainer ka banaya
 * batch PENDING state me DB me row bana leta hai.
 *
 * Iska matlab: normal list queries me PENDING batches
 * DIKHNE NAHI CHAHIYE. Warna approve hone se pehle hi
 * batch students ko dikhne lagega.
 *
 * Isliye neeche zyadatar methods me "ApprovalStatus"
 * ka filter laga hua hai. Isko skip mat karna.
 * ============================================================================
 */
@Repository
public interface BatchRepository extends JpaRepository<Batch, Long> {

    // ========================================================================
    // 1. FIND  --- ek batch
    // ========================================================================

    /**
     * Business ID se batch dhundo.
     *
     * Example: findByBatchId("JAVA101")
     */
    Optional<Batch> findByBatchId(String batchId);

    /**
     * Batch + Trainer ek hi query me.
     *
     * JOIN FETCH KYUN?
     * -----------------------------------------------------------------------
     * Batch me trainer LAZY hai. Normal findByBatchId() ke baad
     * batch.getTrainer().getName() karo to Hibernate ek
     * EXTRA QUERY maar dega.
     *
     * Ek batch ke liye 2 query = theek hai.
     * 50 batch ki list me 50 extra query = N+1 problem = slow.
     *
     * JOIN FETCH ek hi query me dono le aata hai.
     *
     * Ye method wahan use karna jahan response me
     * trainer ka naam bhi chahiye ho.
     */
    @Query("SELECT b FROM Batch b LEFT JOIN FETCH b.trainer WHERE b.batchId = :batchId")
    Optional<Batch> findByBatchIdWithTrainer(@Param("batchId") String batchId);


    // ========================================================================
    // 2. EXISTS  --- duplicate check
    // ========================================================================

    boolean existsByBatchId(String batchId);

    /**
     * Same naam ka batch pehle se to nahi?
     *
     * Ye HARD ERROR nahi hai — "Java Full Stack" naam ke
     * do batch ho sakte hain (morning aur evening).
     *
     * Isliye service isse sirf WARNING dikhane ke liye
     * use karegi, block karne ke liye nahi.
     */
    boolean existsByBatchNameIgnoreCase(String batchName);


    // ========================================================================
    // 3. BATCH ID GENERATE  --- IdGeneratorUtil ke liye
    // ========================================================================
    /**
     * Kisi PREFIX ki sabse nayi batch ID nikalta hai.
     *
     * Trainer ki tarah simple nahi hai, kyunki batch ID
     * naam se banti hai:
     *
     *   "Java Full Stack" -> JAVA101, JAVA102 ...
     *   "MERN Stack"      -> MERN101, MERN102 ...
     *
     * Matlab har prefix ka apna counter chalta hai.
     *
     * Service me aise use hoga:
     *   String prefix = IdGeneratorUtil.batchPrefix("Java Full Stack");  // JAVA
     *   String lastId = batchRepository.findLastBatchIdByPrefix(prefix).orElse(null);
     *   String newId  = IdGeneratorUtil.nextBatchId("Java Full Stack", lastId);
     *
     * CONCAT(:prefix, '%')
     * -----------------------------------------------------------------------
     * SQL me isse "JAVA%" banta hai — matlab JAVA se
     * shuru hone wali saari IDs.
     *
     * ORDER BY b.id DESC kyun?
     * -----------------------------------------------------------------------
     * Wahi reason jo TrainerRepository me tha — batchId
     * string hai, aur string sorting me "JAVA9" > "JAVA10"
     * aa jata hai (galat). DB ka numeric id hamesha sahi
     * order deta hai.
     */
    @Query("""
           SELECT b.batchId FROM Batch b
           WHERE b.batchId LIKE CONCAT(:prefix, '%')
           ORDER BY b.id DESC
           LIMIT 1
           """)
    Optional<String> findLastBatchIdByPrefix(@Param("prefix") String prefix);


    // ========================================================================
    // 4. LIST  --- admin ke liye
    // ========================================================================

    /**
     * Sirf APPROVED batches.
     *
     * Admin dashboard ki main list.
     * PENDING wale yahan nahi dikhenge.
     */
    Page<Batch> findByApprovalStatus(ApprovalStatus approvalStatus, Pageable pageable);

    /**
     * Approval status + batch status dono ka filter.
     *
     * Example: APPROVED + ACTIVE  ->  abhi chal rahe batches
     */
    Page<Batch> findByApprovalStatusAndStatus(
            ApprovalStatus approvalStatus, BatchStatus status, Pageable pageable);


    // ========================================================================
    // 5. TRAINER KE BATCHES
    // ========================================================================

    /**
     * Ek trainer ke saare approved batches.
     *
     * API 8 me bahut kaam aayega — "ALL_MY_BATCHES"
     * announcement ke liye trainer ke saare batches
     * chahiye honge.
     */
    List<Batch> findByTrainerAndApprovalStatus(Trainer trainer, ApprovalStatus approvalStatus);

    /**
     * Wahi cheez, lekin paginated (trainer ke dashboard ke liye).
     */
    Page<Batch> findByTrainerAndApprovalStatus(
            Trainer trainer, ApprovalStatus approvalStatus, Pageable pageable);

    /**
     * Trainer ke saare batches — har status ka.
     *
     * Trainer apne PENDING batches bhi dekh sake,
     * taaki pata chale approval aaya ya nahi.
     */
    Page<Batch> findByTrainer(Trainer trainer, Pageable pageable);

    /**
     * Ye batch iss trainer ka hai ya nahi.
     *
     * SECURITY CHECK ke liye zaroori.
     *
     * Kyunki abhi login nahi hai, trainer koi bhi batchId
     * bhej sakta hai. Announcement ya content upload
     * karne se pehle ye check karna zaroori hai ki
     * batch usi ka hai.
     *
     * Bina iske trainer A, trainer B ke batch me
     * announcement bhej sakta hai.
     */
    boolean existsByBatchIdAndTrainer(String batchId, Trainer trainer);


    // ========================================================================
    // 6. SEARCH  --- API 10
    // ========================================================================
    /**
     * API 10 --- "trainer ke naam se ya current topic se batch dhundo"
     *
     * DONO OPTIONAL HAIN
     * -----------------------------------------------------------------------
     * Admin sirf naam de sakta hai, sirf topic de sakta hai,
     * ya dono de sakta hai.
     *
     * ":param IS NULL OR condition" ka trick
     * -----------------------------------------------------------------------
     * Agar parameter null hai -> pehli condition true ->
     * poora OR true -> matlab wo filter LAGA HI NAHI.
     *
     * Isse ek hi query se 4 alag-alag search kaam kar jate hain:
     *   naam only / topic only / dono / kuch bhi nahi
     *
     * Warna 4 alag methods likhne padte, ya Specification API
     * jaisa complex system banana padta.
     *
     * LOWER(...) LIKE LOWER(...)
     * -----------------------------------------------------------------------
     * Chhote-bade letter ka farak khatam. "java" likho ya
     * "JAVA" — dono chalega.
     *
     * NOTE : Phase 8 me iska aur bada version banayenge
     * (trainer ki details ke saath). Ye base hai.
     */
    @Query("""
           SELECT b FROM Batch b
           LEFT JOIN b.trainer t
           WHERE b.approvalStatus = com.uncodemy.lms.model.enums.ApprovalStatus.APPROVED
             AND (:trainerName IS NULL OR LOWER(t.name) LIKE LOWER(CONCAT('%', :trainerName, '%')))
             AND (:topic       IS NULL OR LOWER(b.currentTopic) LIKE LOWER(CONCAT('%', :topic, '%')))
           """)
    Page<Batch> searchBatches(@Param("trainerName") String trainerName,
                              @Param("topic") String topic,
                              Pageable pageable);


    // ========================================================================
    // 7. COUNT  --- dashboard stats
    // ========================================================================

    /**
     * Ek trainer ke kitne approved batches hain.
     *
     * TrainerResponse me "totalBatches" isse bharega.
     *
     * DHYAN: trainer.getBatches().size() NAHI karna —
     * wo poori list memory me load kar deta hai.
     * Ye query sirf number lati hai.
     */
    long countByTrainerAndApprovalStatus(Trainer trainer, ApprovalStatus approvalStatus);

    /** Kitne batches approved hain */
    long countByApprovalStatus(ApprovalStatus approvalStatus);

    /** Kitne batches abhi chal rahe hain */
    long countByStatusAndApprovalStatus(BatchStatus status, ApprovalStatus approvalStatus);


    // ========================================================================
    // 8. BULK  --- mail ke liye
    // ========================================================================
    /**
     * Ek batch ke saare ACTIVE students ke email.
     *
     * API 8 (announcement) aur API 9 (content) ki jaan.
     *
     * TEEN FILTER LAGE HAIN — teeno zaroori hain:
     *
     *   sb.active = true            -> student ne batch chhoda to nahi
     *   s.approvalStatus = APPROVED -> trainer ka add kiya student
     *                                  jo abhi approve nahi hua,
     *                                  usko mail nahi jani chahiye
     *   s.status = ACTIVE           -> block/inactive student ko nahi
     *
     * Ek bhi filter chhoot gaya to galat logo ko mail chali jayegi.
     *
     * NOTE: student ka status enum "StudentStatus" hai —
     * agar tumhare enum me ACTIVE ke alawa naam hai
     * to yahan badalna padega.
     */
    @Query("""
           SELECT s.email FROM StudentBatch sb
           JOIN sb.student s
           WHERE sb.batch.batchId = :batchId
             AND sb.active = true
             AND s.approvalStatus = com.uncodemy.lms.model.enums.ApprovalStatus.APPROVED
             AND s.status = com.uncodemy.lms.model.enums.StudentStatus.ACTIVE
           """)
    List<String> findActiveStudentEmailsByBatchId(@Param("batchId") String batchId);

    /**
     * Ek TRAINER ke SAARE batches ke students ke email.
     *
     * API 8 ka "ALL_MY_BATCHES" scope isi se chalega.
     *
     * DISTINCT KYUN?
     * -----------------------------------------------------------------------
     * Ek student trainer ke DO batches me ho sakta hai
     * (Java bhi padh raha, Spring bhi).
     *
     * Bina DISTINCT ke usko same announcement ki
     * DO MAIL jayengi. Distinct se ek hi jayegi.
     */
    @Query("""
           SELECT DISTINCT s.email FROM StudentBatch sb
           JOIN sb.student s
           JOIN sb.batch b
           WHERE b.trainer.trainerId = :trainerId
             AND b.approvalStatus = com.uncodemy.lms.model.enums.ApprovalStatus.APPROVED
             AND sb.active = true
             AND s.approvalStatus = com.uncodemy.lms.model.enums.ApprovalStatus.APPROVED
             AND s.status = com.uncodemy.lms.model.enums.StudentStatus.ACTIVE
           """)
    List<String> findActiveStudentEmailsByTrainerId(@Param("trainerId") String trainerId);
}