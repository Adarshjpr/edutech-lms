package com.uncodemy.lms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ============================================================================
 * TrainerCreateResponse   ---  SIRF create API ke liye
 * ============================================================================
 *
 * POST /api/admin/trainers  ka response.
 *
 * Sample JSON:
 * ---------------------------------------------------------------------------
 * {
 *   "trainer" : { ...TrainerResponse... },
 *   "temporaryPassword" : "Kf7@mQx2",
 *   "mailSentTo" : "rahul@gmail.com",
 *   "note" : "Password sirf abhi dikh raha hai..."
 * }
 *
 * YAHI EK JAGAH HAI JAHAN PLAIN PASSWORD DIKHTA HAI
 * ---------------------------------------------------------------------------
 * DB me sirf BCrypt hash jata hai. Plain password
 * kahin store nahi hota.
 *
 * Ye response me isliye bhej rahe hain kyunki:
 *   - Mail kabhi-kabhi spam me chali jati hai
 *   - SES down ho sakta hai
 *   - Admin turant WhatsApp pe bhej sake
 *
 * Ek baar ye screen band, to password gaya —
 * phir reset hi karna padega.
 *
 * ALAG CLASS KYUN BANAYI?
 * ---------------------------------------------------------------------------
 * TrainerResponse me hi nullable password field rakh sakte the.
 *
 * Lekin phir kal ko koi developer list API me galti se
 * password bhi set kar deta, ya update API me —
 * aur password leak ho jata.
 *
 * Alag class hone se ye galti karna MUMKIN HI NAHI.
 * Password field sirf isi class me hai, aur ye class
 * sirf create endpoint use karta hai.
 * ============================================================================
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainerCreateResponse {

    /** Bana hua trainer (bina password ke) */
    private TrainerResponse trainer;

    /**
     * System ka banaya hua password — PLAIN TEXT.
     *
     * Sirf isi response me. DB me hash hai.
     */
    private String temporaryPassword;

    /** Kis email pe credentials bheji gayi */
    private String mailSentTo;

    /** Admin ko dikhane ke liye warning */
    @Builder.Default
    private String note =
            "Password sirf abhi dikh raha hai. Trainer ko mail bhej di gayi hai. "
            + "Zarurat ho to abhi copy kar lein — dobara nahi dikhega.";
}