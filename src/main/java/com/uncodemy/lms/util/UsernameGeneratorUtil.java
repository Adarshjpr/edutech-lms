package com.uncodemy.lms.util;

import java.util.Locale;
import java.util.function.Predicate;

/**
 * ============================================================================
 * UsernameGeneratorUtil
 * ============================================================================
 *
 * API 2 --- Trainer ko mail me USERNAME + PASSWORD jayega.
 *
 * FORMULA :  pehla naam (lowercase)  +  phone ke last 4 digit
 *
 *   "Rahul Sharma"  + 9876543210  ->  rahul3210
 *   "Amit"          + 9123456789  ->  amit6789
 *   "Neha Gupta"    + 9000012345  ->  neha2345
 *
 * NAAM ke saath PHONE kyun mila rahe hain?
 * ---------------------------------------------------------------------------
 * Sirf naam se username banate to "Rahul Sharma" aur "Rahul Verma"
 * dono ka "rahul" ban jata -> clash.
 *
 * Phone har banda ka alag hota hai, isliye last 4 digit
 * jodne se clash ki chance na ke barabar ho jati hai.
 *
 * Aur trainer ko yaad rakhna bhi aasan hai —
 * "mera naam + mere number ke last 4 digit".
 *
 * YE CLASS DB SE BAAT NAHI KARTI
 * ---------------------------------------------------------------------------
 * IdGeneratorUtil ki tarah ye bhi pure utility hai.
 *
 * Lekin username unique hona zaroori hai, isliye ek method
 * aisa bhi diya hai jisme service apna "ye username exist
 * karta hai kya?" wala check pass kar sakti hai.
 * ============================================================================
 */
public final class UsernameGeneratorUtil {

    private UsernameGeneratorUtil() {
        throw new UnsupportedOperationException("Utility class hai, object mat banao");
    }

    /** Phone ke kitne last digits lene hain */
    private static final int PHONE_DIGITS = 4;

    /** Naam wala hissa itne se lamba nahi hoga */
    private static final int MAX_NAME_PART = 10;

    /** Naam ya phone dono na mile to ye fallback */
    private static final String FALLBACK_NAME = "user";


    // ========================================================================
    // MAIN METHOD  (simple version)
    // ========================================================================
    /**
     * Naam aur phone se username banata hai.
     *
     *   generate("Rahul Sharma", "9876543210")  ->  "rahul3210"
     *   generate("Amit", "+91 91234-56789")     ->  "amit6789"
     *   generate("Rahul Sharma", null)          ->  "rahul" (phone nahi hai)
     *
     * NOTE: ye uniqueness check NAHI karta.
     * Uske liye neeche wala generateUnique() use karo.
     */
    public static String generate(String name, String phone) {

        String namePart  = cleanName(name);
        String phonePart = lastDigits(phone);

        return namePart + phonePart;
    }


    // ========================================================================
    // MAIN METHOD  (unique version)  ---  SERVICE ISI KO USE KAREGI
    // ========================================================================
    /**
     * Username banata hai AUR unique bhi karta hai.
     *
     * Agar "rahul3210" already le liya gaya hai, to:
     *   rahul3210  -> busy
     *   rahul32101 -> try
     *   rahul32102 -> try
     *   ... jab tak khali na mile
     *
     * SERVICE ME AISE USE HOGA:
     * ---------------------------------------------------------------------
     *   String username = UsernameGeneratorUtil.generateUnique(
     *           request.getName(),
     *           request.getPhone(),
     *           trainerRepository::existsByUsername
     *   );
     *
     * "trainerRepository::existsByUsername" ka matlab —
     * "ye method use kar ke check karna ki username busy hai ya nahi".
     *
     * Isse ye class repository pe depend kiye bina bhi
     * uniqueness handle kar leti hai.
     *
     * @param name       trainer ka naam
     * @param phone      trainer ka phone
     * @param existsCheck ek function jo bataye ki username busy hai ya nahi
     */
    public static String generateUnique(String name,
                                        String phone,
                                        Predicate<String> existsCheck) {

        String base = generate(name, phone);

        // Pehli koshish — bina kisi number ke
        if (existsCheck == null || !existsCheck.test(base)) {
            return base;
        }

        // Busy hai -> peeche number lagate jao
        // 1000 tak try karenge, uske aage practically kabhi nahi jayega
        for (int counter = 1; counter <= 1000; counter++) {

            String candidate = base + counter;

            if (!existsCheck.test(candidate)) {
                return candidate;
            }
        }

        // Yahan tak pahunchna almost impossible hai,
        // lekin silently galat username dene se better hai saaf bata dena
        throw new IllegalStateException(
                "Username generate nahi ho paya. 1000 baar try kiya : " + base);
    }


    // ========================================================================
    // HELPERS
    // ========================================================================

    /**
     * Naam se pehla word nikalta hai aur saaf karta hai.
     *
     *   "Rahul Sharma"    -> rahul
     *   "  neha  gupta "  -> neha
     *   "Md. Salman"      -> md          (dot hat gaya)
     *   "R@hul"           -> rhul        (symbol hat gaya)
     *   "Chandrashekhar"  -> chandrashe  (10 pe cut)
     *   null / ""         -> user        (fallback)
     *
     * Sirf a-z rakhte hain kyunki username me space,
     * dot, ya symbol rakhna aage dikkat karta hai.
     */
    private static String cleanName(String name) {

        if (name == null || name.isBlank()) {
            return FALLBACK_NAME;
        }

        // Pehla word
        String firstWord = name.trim().split("\\s+")[0];

        // Sirf letters
        String letters = firstWord.replaceAll("[^A-Za-z]", "");

        if (letters.isEmpty()) {
            return FALLBACK_NAME;
        }

        // Bahut lamba naam ho to kaat do
        if (letters.length() > MAX_NAME_PART) {
            letters = letters.substring(0, MAX_NAME_PART);
        }

        return letters.toLowerCase(Locale.ROOT);
    }

    /**
     * Phone ke last 4 digit nikalta hai.
     *
     *   "9876543210"       -> 3210
     *   "+91 98765-43210"  -> 3210     (+, space, - sab hat gaye)
     *   "0091 9876543210"  -> 3210
     *   "123"              -> 123      (4 se kam hai to jitne hain utne)
     *   null               -> ""       (kuch nahi)
     *
     * Pehle saare non-digit characters hata dete hain,
     * kyunki log phone alag-alag format me likhte hain.
     */
    private static String lastDigits(String phone) {

        if (phone == null || phone.isBlank()) {
            return "";
        }

        // Sirf 0-9 rakho
        String digits = phone.replaceAll("\\D", "");

        if (digits.isEmpty()) {
            return "";
        }

        if (digits.length() <= PHONE_DIGITS) {
            return digits;
        }

        return digits.substring(digits.length() - PHONE_DIGITS);
    }
}