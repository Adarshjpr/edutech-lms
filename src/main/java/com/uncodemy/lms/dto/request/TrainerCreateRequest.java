package com.uncodemy.lms.dto.request;

import com.uncodemy.lms.model.enums.TrainerRole;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ============================================================================
 * TrainerCreateRequest   ---  API 1 ka INPUT
 * ============================================================================
 *
 * POST /api/admin/trainers  ki request body.
 *
 * Sample JSON:
 * ---------------------------------------------------------------------------
 * {
 *   "name"        : "Rahul Sharma",
 *   "username"    : "rahul.sharma",
 *   "email"       : "rahul@gmail.com",
 *   "designation" : "Senior Java Trainer",
 *   "phone"       : "9876543210",
 *   "role"        : "TRAINER"
 * }
 *
 * ENTITY KO DIRECTLY REQUEST BODY KYUN NAHI BANAYA?
 * ---------------------------------------------------------------------------
 * Agar @RequestBody Trainer likh dete, to user JSON me
 * ye sab bhi bhej sakta tha:
 *
 *   "id"       : 5          -> kisi aur ki row overwrite
 *   "password" : "abc"      -> apni marzi ka password set
 *   "active"   : true       -> banned account wapas chalu
 *
 * DTO me sirf WOHI fields hain jo user ko bhejne ki
 * permission hai. Baaki sab service khud set karti hai.
 *
 * KYA-KYA USER NAHI BHEJTA (service khud banati hai):
 * ---------------------------------------------------------------------------
 *   trainerId   -> IdGeneratorUtil banayega (TR101, TR102...)
 *   password    -> PasswordGeneratorUtil banayega, mail me jayega
 *   firstLogin  -> hamesha true
 *   active      -> hamesha true
 *   createdAt   -> JPA Auditing bharega
 * ============================================================================
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainerCreateRequest {

    /**
     * Trainer ka pura naam.
     *
     * API 10 me isi naam se search hoga.
     *
     * @NotBlank
     * -----------------------------------------------------------------------
     * @NotNull se alag hai:
     *
     *   @NotNull  -> ""    chalega  (khali string bhi valid)
     *   @NotEmpty -> "   " chalega  (sirf space bhi valid)
     *   @NotBlank -> "   " bhi FAIL  <-- yahi chahiye
     *
     * Text fields pe hamesha @NotBlank use karo.
     */
    @NotBlank(message = "Trainer ka naam zaroori hai")
    @Size(min = 2, max = 100, message = "Naam 2 se 100 character ke beech hona chahiye")
    private String name;

    /**
     * Login Username --- ADMIN KHUD DEGA
     *
     * (Tumne bola tha ki username user hi rakhega,
     *  system auto-generate na kare.)
     *
     * REGEX SAMJHO : ^[a-z0-9._]+$
     * -----------------------------------------------------------------------
     *   ^        -> shuruaat
     *   [a-z]    -> sirf CHHOTE letters (capital allowed nahi)
     *   0-9      -> numbers
     *   . _      -> dot aur underscore
     *   +        -> ek ya zyada character
     *   $        -> khatam
     *
     * Matlab ye chalega    : rahul.sharma, amit_kumar, java_trainer2
     * Ye nahi chalega      : Rahul Sharma (space + capital)
     *                        rahul@lms   (@ allowed nahi)
     *                        राहुल        (english hi chalegi)
     *
     * SIRF LOWERCASE KYUN?
     * -----------------------------------------------------------------------
     * Warna "Rahul" aur "rahul" do alag account ban jate,
     * aur login karte waqt confusion hota.
     *
     * Service phir bhi .toLowerCase() lagayegi — double safety.
     *
     * NOTE: username DB me exist karta hai ya nahi,
     * wo yahan check NAHI hota. Ye sirf FORMAT check karta hai.
     * Duplicate ka check service layer karegi
     * (existsByUsername) aur 409 Conflict dega.
     */
    @NotBlank(message = "Username zaroori hai")
    @Size(min = 4, max = 30, message = "Username 4 se 30 character ka hona chahiye")
    @Pattern(
            regexp = "^[a-z0-9._]+$",
            message = "Username me sirf chhote letters, numbers, dot (.) aur underscore (_) chalenge"
    )
    private String username;

    /**
     * Trainer ki Email.
     *
     * Ispe hi credentials wali mail jayegi (API 2),
     * isliye ye sahi honi bahut zaroori hai.
     *
     * @Email ka honest sach
     * -----------------------------------------------------------------------
     * Ye sirf FORMAT check karta hai — "kuch@kuch.kuch" hai ya nahi.
     *
     * Ye check NAHI karta ki:
     *   ✘ email asli hai ya nahi
     *   ✘ inbox exist karta hai ya nahi
     *
     * "abcd@xyz.com" format se sahi hai, lekin ho sakta hai
     * aisa koi mailbox ho hi na.
     *
     * Isliye mail jane ke baad SES ke logs / bounce dekhna zaroori hai.
     */
    @NotBlank(message = "Email zaroori hai")
    @Email(message = "Email ka format sahi nahi hai")
    @Size(max = 150, message = "Email bahut lambi hai")
    private String email;

    /**
     * Designation --- OPTIONAL
     *
     * Example: Senior Java Trainer, MERN Trainer
     *
     * Ispe @NotBlank nahi hai, matlab null bhej sakte ho.
     * Lekin agar bheja to 100 character se lamba nahi ho.
     */
    @Size(max = 100, message = "Designation 100 character se lambi nahi ho sakti")
    private String designation;

    /**
     * Mobile Number --- OPTIONAL
     *
     * REGEX : ^[0-9]{10,15}$
     *   Sirf digits, 10 se 15 tak.
     *
     * Indian number 10 digit ka hota hai,
     * lekin country code (+91) ke saath 12 ho jata hai —
     * isliye 15 tak allow kiya hai.
     *
     * NOTE: "+" allowed nahi hai. Agar "+919876543210"
     * bhejna ho to regex me "^\\+?[0-9]{10,15}$" kar dena.
     */
    @Pattern(
            regexp = "^[0-9]{10,15}$",
            message = "Phone number me sirf 10 se 15 digits hone chahiye"
    )
    private String phone;

    /**
     * Trainer ka Role.
     *
     * Values: TRAINER, ADMIN, PLACEMENT_TEAM
     * (jo tumhare TrainerRole enum me hain)
     *
     * @NotNull kyun, @NotBlank kyun nahi?
     * -----------------------------------------------------------------------
     * @NotBlank sirf String pe lagta hai. Ye enum hai,
     * isliye @NotNull.
     *
     * GALAT VALUE BHEJI TO KYA HOGA?
     * -----------------------------------------------------------------------
     * "role" : "TRAINERR"  (extra R)
     *
     * -> Jackson JSON parse hi nahi kar payega
     * -> HttpMessageNotReadableException throw hogi
     * -> GlobalExceptionHandler ka Handler #5 use pakdega
     * -> User ko milega : "Request body padhi nahi ja saki.
     *                      JSON format ya enum value check karo."
     *
     * Matlab ye case pehle se handle hai. ✔
     */
    @NotNull(message = "Role zaroori hai (TRAINER / ADMIN / PLACEMENT_TEAM)")
    private TrainerRole role;
}