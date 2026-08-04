package com.uncodemy.lms.service.rule;

import com.uncodemy.lms.dto.request.TrainerCreateRequest;
import com.uncodemy.lms.dto.response.TrainerCreateResponse;
import com.uncodemy.lms.dto.response.TrainerResponse;
import com.uncodemy.lms.model.Trainer;
import com.uncodemy.lms.model.enums.TrainerRole;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * ============================================================================
 * TrainerService  (Interface)
 * ============================================================================
 *
 * Trainer se related saara BUSINESS LOGIC.
 *
 * Controller sirf isse baat karega — TrainerServiceImpl ka
 * naam tak nahi jaanega.
 *
 * INTERFACE + IMPL KA PATTERN KYUN?
 * ---------------------------------------------------------------------------
 * Bahut log bolte hain "ek hi implementation hai to interface
 * kyun banaya?" — sahi sawal hai.
 *
 * Iss project me fayda ye hai:
 *
 *   ✔ Controller padhne wale ko yahi ek file dekhni hai —
 *     pura pata chal jata hai ki kya-kya ho sakta hai
 *   ✔ Test me FakeTrainerService laga sakte hain
 *   ✔ @Transactional / @Async proxy theek se lagte hain
 *
 * Aur sabse bada: SIGNATURE aur LOGIC alag rehta hai.
 * Iss file me sirf "kya hoga" hai, "kaise hoga" nahi.
 * ============================================================================
 */
public interface TrainerService {

    // ========================================================================
    // API 1 + 2  --- CREATE
    // ========================================================================

    /**
     * Naya Trainer banata hai aur usko credentials mail karta hai.
     *
     * ANDAR KYA-KYA HOGA:
     * -----------------------------------------------------------------------
     *  1. Username duplicate to nahi?      -> 409 Conflict
     *  2. Email duplicate to nahi?         -> 409 Conflict
     *  3. trainerId banao                  -> TR101, TR102 ...
     *  4. Random password banao            -> Kf7@mQx2
     *  5. Password ko BCrypt hash karo     -> DB me hash
     *  6. DB me save karo
     *  7. Mail bhejo (background me)       -> username + plain password
     *  8. Response do (plain password ke saath, sirf ek baar)
     *
     * @throws com.uncodemy.lms.exception.DuplicateResourceException
     *         agar username ya email pehle se hai
     */
    TrainerCreateResponse createTrainer(TrainerCreateRequest request);


    // ========================================================================
    // USERNAME CHECK  --- form bharte waqt
    // ========================================================================

    /**
     * Username available hai ya nahi.
     *
     * Frontend isse form me live check karega —
     * save dabane se pehle hi green tick / red cross dikha dega.
     *
     * @return true = free hai, le sakte ho
     *         false = koi aur le chuka hai
     */
    boolean isUsernameAvailable(String username);


    // ========================================================================
    // READ
    // ========================================================================

    /**
     * Business ID se trainer nikalo.
     *
     * Example: getByTrainerId("TR101")
     *
     * @throws com.uncodemy.lms.exception.ResourceNotFoundException
     *         agar trainer na mile
     */
    TrainerResponse getByTrainerId(String trainerId);

    /**
     * Saare active trainers — page ke hisaab se.
     *
     * @param role  optional filter. null bheja to sab aayenge.
     */
    Page<TrainerResponse> getAllTrainers(TrainerRole role, Pageable pageable);

    /**
     * Naam se search.
     *
     * API 10 ka simple version — admin "rah" likhe
     * to "Rahul Sharma" aa jaye.
     */
    Page<TrainerResponse> searchByName(String name, Pageable pageable);


    // ========================================================================
    // UPDATE
    // ========================================================================

    /**
     * Trainer ko active / inactive karo (soft delete).
     *
     * DELETE KYUN NAHI KARTE?
     * -----------------------------------------------------------------------
     * Trainer ke saath uske batches, announcements aur
     * contents jude hote hain.
     *
     * Row delete karte to ya to sab kuch delete ho jata,
     * ya foreign key error aati.
     *
     * Isliye bas active = false. Data safe, trainer band.
     *
     * @param active true = chalu, false = band
     */
    TrainerResponse updateStatus(String trainerId, boolean active);


    // ========================================================================
    // INTERNAL  --- doosri services ke liye
    // ========================================================================

    /**
     * Trainer ki ENTITY nikalta hai (DTO nahi).
     *
     * YE CONTROLLER KE LIYE NAHI HAI.
     *
     * Ye BatchService, AnnouncementService jaisi doosri
     * services ke liye hai — unhe asli entity chahiye hoti
     * hai taaki relation set kar sakein:
     *
     *     Batch batch = new Batch();
     *     batch.setTrainer(trainerService.getEntityOrThrow("TR101"));
     *
     * DTO se ye kaam nahi ho sakta.
     *
     * Har service me alag-alag findByTrainerId().orElseThrow(...)
     * likhne se better hai ki ek hi jagah ho.
     *
     * @throws com.uncodemy.lms.exception.ResourceNotFoundException
     */
    Trainer getEntityOrThrow(String trainerId);
}