package com.uncodemy.lms.repository;

import com.uncodemy.lms.model.Trainer;
import com.uncodemy.lms.model.enums.TrainerRole;

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
 * TrainerRepository
 * ============================================================================
 *
 * Trainer table ki saari database queries yahan.
 *
 * JpaRepository<Trainer, Long>
 * ---------------------------------------------------------------------------
 * Trainer -> kis entity ke liye
 * Long    -> uski primary key ka type
 *
 * Isse ye methods MUFT me mil jate hain (likhne nahi padte):
 *
 *   save(trainer)          -> insert ya update
 *   findById(1L)           -> id se dhundo
 *   findAll()              -> sab
 *   findAll(pageable)      -> page ke hisaab se
 *   deleteById(1L)         -> delete
 *   count()                -> kitne hain
 *   existsById(1L)         -> hai ya nahi
 *
 * METHOD KA NAAM HI QUERY HAI
 * ---------------------------------------------------------------------------
 * Spring Data method ke naam se khud SQL bana leta hai:
 *
 *   findByEmail(email)          -> WHERE email = ?
 *   existsByUsername(username)  -> SELECT count(*) ... WHERE username = ?
 *   findByRoleAndActiveTrue(r)  -> WHERE role = ? AND active = true
 *
 * Implementation class banane ki zarurat NAHI —
 * Spring runtime pe khud bana deta hai.
 * ============================================================================
 */
@Repository
public interface TrainerRepository extends JpaRepository<Trainer, Long> {

    // ========================================================================
    // 1. FIND  --- ek trainer dhundhne ke liye
    // ========================================================================

    /**
     * Business ID se trainer dhundo.
     *
     * Example: findByTrainerId("TR101")
     *
     * Optional<Trainer> kyun?
     * -----------------------------------------------------------------------
     * Kyunki ho sakta hai trainer mile hi na.
     * Optional se NullPointerException se bach jate hain.
     *
     * Service me aise use hoga:
     *   trainerRepository.findByTrainerId("TR101")
     *       .orElseThrow(() -> new ResourceNotFoundException("Trainer", "trainerId", "TR101"));
     */
    Optional<Trainer> findByTrainerId(String trainerId);

    /**
     * Username se dhundo.
     * Aage login banayenge to yahi kaam aayegi.
     */
    Optional<Trainer> findByUsername(String username);

    /**
     * Email se dhundo.
     */
    Optional<Trainer> findByEmail(String email);


    // ========================================================================
    // 2. EXISTS  --- duplicate check ke liye
    // ========================================================================
    /**
     * Ye methods poori entity load NAHI karti,
     * sirf "hai ya nahi" batati hai (SELECT count).
     *
     * Isliye findBy... se fast hai jab sirf
     * check karna ho.
     */

    /** Trainer create karte waqt username duplicate to nahi? */
    boolean existsByUsername(String username);

    /** Email duplicate to nahi? */
    boolean existsByEmail(String email);

    /** trainerId duplicate to nahi? (race condition ka extra check) */
    boolean existsByTrainerId(String trainerId);


    // ========================================================================
    // 3. LAST ID  --- IdGeneratorUtil ke liye
    // ========================================================================
    /**
     * DB me abhi tak ki SABSE NAYI trainerId nikalta hai.
     *
     * Service me aise use hoga:
     *   String lastId = trainerRepository.findLastTrainerId().orElse(null);
     *   String newId  = IdGeneratorUtil.nextTrainerId(lastId);   // TR102
     *
     * ORDER BY t.id DESC kyun, trainerId DESC kyun nahi?
     * -----------------------------------------------------------------------
     * Kyunki trainerId ek STRING hai, aur string ki sorting
     * alphabet ke hisaab se hoti hai — number ke hisaab se nahi.
     *
     * String sorting me :
     *   "TR9"  >  "TR10"      <-- galat!
     *   ("9" letter "1" se bada hai)
     *
     * Lekin database ka "id" number hai, aur wo hamesha
     * badhta rehta hai. To sabse nayi row hamesha
     * sabse badi id wali hogi.
     *
     * Isliye id se sort kar rahe hain.
     */
    @Query("SELECT t.trainerId FROM Trainer t ORDER BY t.id DESC LIMIT 1")
    Optional<String> findLastTrainerId();


    // ========================================================================
    // 4. LIST  --- admin dashboard ke liye
    // ========================================================================

    /**
     * Sirf active trainers, page ke hisaab se.
     *
     * Pageable kyun?
     * -----------------------------------------------------------------------
     * 500 trainers ho gaye to findAll() sabko ek saath
     * memory me le aayega — app slow ho jayega.
     *
     * Pageable se sirf 20-20 aate hain.
     *
     * Controller me:
     *   @PageableDefault(size = 20, sort = "id") Pageable pageable
     */
    Page<Trainer> findByActiveTrue(Pageable pageable);

    /**
     * Role ke hisaab se active trainers.
     *
     * Example: sirf PLACEMENT_TEAM wale chahiye.
     */
    Page<Trainer> findByRoleAndActiveTrue(TrainerRole role, Pageable pageable);


    // ========================================================================
    // 5. SEARCH  --- API 10 ke liye
    // ========================================================================
    /**
     * API 10 --- "admin trainer ke naam likhe aur trainer aa jaye"
     *
     * ContainingIgnoreCase
     * -----------------------------------------------------------------------
     * Containing     -> LIKE %rahul%   (beech me kahin bhi mile)
     * IgnoreCase     -> chhote-bade letter se farak nahi padta
     *
     * "rah" likho -> "Rahul Sharma" aur "Prahlad" dono aayenge.
     *
     * NOTE: Phase 8 me iska bada version banayenge jisme
     * naam + current topic dono se search hoga. Ye simple wala
     * abhi trainer list filter karne ke liye hai.
     */
    Page<Trainer> findByNameContainingIgnoreCaseAndActiveTrue(String name, Pageable pageable);


    // ========================================================================
    // 6. BULK FETCH  --- mail bhejne ke liye
    // ========================================================================
    /**
     * Saare active trainers ke email.
     *
     * Poori Trainer entity nikalne ki jagah sirf email
     * nikal rahe hain — kyunki mail bhejne ke liye
     * bas email chahiye, baaki data bekaar memory legi.
     */
    @Query("SELECT t.email FROM Trainer t WHERE t.active = true")
    List<String> findAllActiveEmails();


    // ========================================================================
    // 7. COUNT  --- dashboard stats
    // ========================================================================

    /** Kitne active trainers hain */
    long countByActiveTrue();

    /** Kis role me kitne hain */
    long countByRoleAndActiveTrue(TrainerRole role);
}