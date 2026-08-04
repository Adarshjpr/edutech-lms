package com.uncodemy.lms.util;

import java.util.Locale;

/**
 * ============================================================================
 * IdGeneratorUtil
 * ============================================================================
 *
 * Human-readable business IDs banane ke liye.
 *
 *   Trainer -> TR101, TR102, TR103 ...
 *   Student -> STU101, STU102 ...
 *   Admin   -> ADM101, ADM102 ...
 *   Batch   -> JAVA101, MERN101, PYTHON101 ...
 *
 * DATABASE KA id (1, 2, 3) SE YE ALAG KYUN?
 * ---------------------------------------------------------------------------
 * DB ka "id" internal hai — usse pata nahi chalta cheez kya hai.
 * "TR101" dekh ke turant samajh aata hai ki trainer hai.
 *
 * Mail me, UI pe, aur support call pe yahi ID kaam aati hai.
 *
 * YE CLASS DB SE BAAT NAHI KARTI
 * ---------------------------------------------------------------------------
 * Ye sirf calculation karti hai. DB se "last ID kya thi"
 * wo service layer nikalegi aur yahan pass karegi.
 *
 * Service me aise use hoga:
 *
 *     String lastId = trainerRepository.findLastTrainerId().orElse(null);
 *     String newId  = IdGeneratorUtil.nextId("TR", lastId, 101);
 *
 * Fayda: is class ko test karne ke liye database ki zarurat hi nahi.
 * ============================================================================
 */
public final class IdGeneratorUtil {

    /**
     * Constructor private hai.
     *
     * Kyun? Kyunki ye utility class hai — iska object banane ka
     * koi matlab nahi. Sab methods static hain.
     *
     * "new IdGeneratorUtil()" likhoge to compile error aayega.
     */
    private IdGeneratorUtil() {
        throw new UnsupportedOperationException("Utility class hai, object mat banao");
    }

    // Default starting numbers
    public static final int TRAINER_START = 101;
    public static final int STUDENT_START = 101;
    public static final int ADMIN_START   = 101;
    public static final int BATCH_START   = 101;

    public static final String TRAINER_PREFIX = "TR";
    public static final String STUDENT_PREFIX = "STU";
    public static final String ADMIN_PREFIX   = "ADM";


    // ========================================================================
    // CORE METHOD
    // ========================================================================
    /**
     * Prefix + last ID se agli ID banata hai.
     *
     * Example:
     *   nextId("TR", "TR101", 101)  ->  "TR102"
     *   nextId("TR", null,    101)  ->  "TR101"     (pehli baar)
     *   nextId("STU", "STU999", 101) -> "STU1000"   (auto badh jayega)
     *
     * @param prefix       "TR", "STU", "ADM" ya batch ka prefix
     * @param lastId       DB me abhi tak ki sabse badi ID (null ho sakti hai)
     * @param startNumber  agar lastId null hai to kahan se shuru karein
     */
    public static String nextId(String prefix, String lastId, int startNumber) {

        if (prefix == null || prefix.isBlank()) {
            throw new IllegalArgumentException("Prefix khali nahi ho sakta");
        }

        String cleanPrefix = prefix.trim().toUpperCase(Locale.ROOT);

        // Pehli baar — table khali hai
        if (lastId == null || lastId.isBlank()) {
            return cleanPrefix + startNumber;
        }

        int lastNumber = extractNumber(lastId);

        // ID mili to thi, lekin usme number nahi tha (galat format)
        // -> safe side pe start se shuru kar dete hain
        if (lastNumber < 0) {
            return cleanPrefix + startNumber;
        }

        return cleanPrefix + (lastNumber + 1);
    }


    // ========================================================================
    // SHORTCUT METHODS  (service me chhota code likhne ke liye)
    // ========================================================================

    /** TR101, TR102 ... */
    public static String nextTrainerId(String lastId) {
        return nextId(TRAINER_PREFIX, lastId, TRAINER_START);
    }

    /** STU101, STU102 ... */
    public static String nextStudentId(String lastId) {
        return nextId(STUDENT_PREFIX, lastId, STUDENT_START);
    }

    /** ADM101, ADM102 ... */
    public static String nextAdminId(String lastId) {
        return nextId(ADMIN_PREFIX, lastId, ADMIN_START);
    }


    // ========================================================================
    // BATCH ID  (thoda alag hai)
    // ========================================================================
    /**
     * Batch ID batch ke NAAM se banti hai.
     *
     *   "Java Full Stack"  ->  JAVA101
     *   "MERN Stack"       ->  MERN101
     *   "Python Django"    ->  PYTHON101
     *
     * Logic:
     *   1. Batch name ka pehla word lo
     *   2. Sirf letters rakho (space, number, symbol hata do)
     *   3. Upper case karo, max 8 letters
     *   4. Uss prefix ki last ID se agla number lagao
     *
     * @param batchName  "Java Full Stack"
     * @param lastId     usi prefix ki last batch id, e.g. "JAVA103" (null ho sakti hai)
     */
    public static String nextBatchId(String batchName, String lastId) {

        String prefix = batchPrefix(batchName);
        return nextId(prefix, lastId, BATCH_START);
    }

    /**
     * Batch name se prefix nikalta hai.
     *
     *   "Java Full Stack"   -> JAVA
     *   "MERN Stack"        -> MERN
     *   "C++ Programming"   -> C          (++ hat jayega)
     *   "  spring boot  "   -> SPRING
     *   ""                  -> BATCH      (fallback)
     *
     * Ye method public hai taaki service "iss prefix ki last ID
     * kya hai" DB se pooch sake.
     */
    public static String batchPrefix(String batchName) {

        if (batchName == null || batchName.isBlank()) {
            return "BATCH";
        }

        // Pehla word nikalo
        String firstWord = batchName.trim().split("\\s+")[0];

        // Sirf A-Z rakho, baaki sab hata do
        String letters = firstWord.replaceAll("[^A-Za-z]", "");

        if (letters.isEmpty()) {
            return "BATCH";
        }

        // 8 se lamba prefix bhadda lagta hai
        if (letters.length() > 8) {
            letters = letters.substring(0, 8);
        }

        return letters.toUpperCase(Locale.ROOT);
    }


    // ========================================================================
    // HELPER
    // ========================================================================
    /**
     * ID ke aakhir me jo number hai wo nikalta hai.
     *
     *   "TR101"    -> 101
     *   "STU1000"  -> 1000
     *   "JAVA101"  -> 101
     *   "TR"       -> -1   (number hai hi nahi)
     *   null       -> -1
     *
     * Peeche se shuru karke jab tak digit milte hain
     * tab tak chalte hain. Isse "C2SHARP101" jaise
     * case me bhi sirf 101 hi milega.
     */
    private static int extractNumber(String id) {

        if (id == null || id.isBlank()) {
            return -1;
        }

        String trimmed = id.trim();
        int end = trimmed.length();
        int start = end;

        // Peeche se digits count karo
        while (start > 0 && Character.isDigit(trimmed.charAt(start - 1))) {
            start--;
        }

        // Ek bhi digit nahi mila
        if (start == end) {
            return -1;
        }

        try {
            return Integer.parseInt(trimmed.substring(start, end));
        } catch (NumberFormatException e) {
            // Number itna bada tha ki int me nahi samaya
            return -1;
        }
    }
}