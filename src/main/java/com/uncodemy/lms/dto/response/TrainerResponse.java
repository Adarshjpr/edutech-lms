package com.uncodemy.lms.dto.response;

import com.uncodemy.lms.model.Trainer;
import com.uncodemy.lms.model.enums.TrainerRole;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * ============================================================================
 * TrainerResponse   ---  API ka OUTPUT
 * ============================================================================
 *
 * Trainer ki information client ko bhejne ke liye.
 *
 * Sample JSON:
 * ---------------------------------------------------------------------------
 * {
 *   "id"          : 1,
 *   "trainerId"   : "TR101",
 *   "name"        : "Rahul Sharma",
 *   "username"    : "rahul.sharma",
 *   "email"       : "rahul@gmail.com",
 *   "designation" : "Senior Java Trainer",
 *   "phone"       : "9876543210",
 *   "role"        : "TRAINER",
 *   "active"      : true,
 *   "firstLogin"  : true,
 *   "totalBatches": 3,
 *   "createdAt"   : "2026-07-31 18:30:00"
 * }
 *
 * ISME PASSWORD KABHI NAHI AAYEGA
 * ---------------------------------------------------------------------------
 * Password ka field yahan HAI HI NAHI. Na plain, na hash.
 *
 * Agar Trainer entity ko seedha return kar dete to
 * password ka hash bhi JSON me chala jata —
 * aur hash bhi expose karna galat hai.
 *
 * ENTITY RETURN KARNE KE AUR NUKSAAN
 * ---------------------------------------------------------------------------
 * Trainer entity me batches, announcements, contents ki
 * LIST hai. Use seedha return karte to:
 *
 *   1. LazyInitializationException aati (session band ho chuka hota)
 *   2. Ya phir poori list JSON me chali jati (bahut bada response)
 *   3. Batch ke andar Trainer, Trainer ke andar Batch...
 *      -> INFINITE LOOP -> StackOverflowError
 *
 * DTO me sirf simple fields hain, koi relation nahi.
 * Ye teeno problem khatam. ✔
 * ============================================================================
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainerResponse {

    /** Database ki internal id */
    private Long id;

    /** Business ID — TR101 */
    private String trainerId;

    private String name;

    /** Login username */
    private String username;

    private String email;

    private String designation;

    private String phone;

    /** TRAINER / ADMIN / PLACEMENT_TEAM */
    private TrainerRole role;

    /** Active hai ya disable kiya gaya */
    private Boolean active;

    /**
     * Abhi tak pehla login karke password change kiya ya nahi.
     *
     * Admin ke liye useful — dekh sakta hai ki
     * trainer ne account use karna shuru kiya ya nahi.
     */
    private Boolean firstLogin;

    /**
     * Iss trainer ke kitne batches hain.
     *
     * Service isse alag se count karke bharegi.
     * Poori batch list bhejne ki zarurat nahi —
     * admin list me sirf number kaafi hai.
     */
    private Integer totalBatches;

    private LocalDateTime createdAt;


    // ========================================================================
    // MAPPER
    // ========================================================================
    /**
     * Entity se DTO banata hai.
     *
     * Ye method yahan (DTO me) kyun rakha, service me kyun nahi?
     * -----------------------------------------------------------------------
     * Kyunki mapping ka code ek hi jagah rahega.
     * Aur service ka code saaf rehta hai:
     *
     *     return TrainerResponse.from(trainer);
     *
     * iski jagah 12 line ka builder likhna padta.
     *
     * MapStruct jaisi library bhi hai jo ye automatic
     * kar deti hai, lekin 5-6 DTO ke liye wo overkill hai.
     *
     * @param trainer      entity
     * @param totalBatches batch ka count (service degi, ya null)
     */
    public static TrainerResponse from(Trainer trainer, Integer totalBatches) {

        if (trainer == null) {
            return null;
        }

        return TrainerResponse.builder()
                .id(trainer.getId())
                .trainerId(trainer.getTrainerId())
                .name(trainer.getName())
                .username(trainer.getUsername())
                .email(trainer.getEmail())
                .designation(trainer.getDesignation())
                .phone(trainer.getPhone())
                .role(trainer.getRole())
                .active(trainer.getActive())
                .firstLogin(trainer.getFirstLogin())
                .totalBatches(totalBatches)
                .createdAt(trainer.getCreatedAt())
                .build();
    }

    /**
     * Shortcut — jab batch count ki zarurat na ho.
     *
     * DHYAN DO: yahan trainer.getBatches().size() NAHI kar rahe.
     *
     * Kyun? Kyunki batches LAZY hai — .size() call karte hi
     * Hibernate DB me ek EXTRA QUERY maar dega.
     *
     * 50 trainers ki list me 50 extra query = N+1 problem.
     * List API bahut slow ho jayegi.
     *
     * Isliye count service layer alag se
     * batchRepository.countByTrainerId() se nikalegi,
     * ek hi query me.
     */
    public static TrainerResponse from(Trainer trainer) {
        return from(trainer, null);
    }
}