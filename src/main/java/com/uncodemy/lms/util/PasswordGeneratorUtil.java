package com.uncodemy.lms.util;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * ============================================================================
 * PasswordGeneratorUtil
 * ============================================================================
 *
 * API 2 --- "trainer ke paas mail jaye username + password"
 *
 * Admin trainer banata hai, system khud ek random password
 * generate karta hai, mail me bhejta hai, aur DB me sirf
 * hash store karta hai.
 *
 * Example output:
 *   Kf7@mQx2
 *   Zp3#nRt9
 *
 * SecureRandom kyun, Random kyun nahi?
 * ---------------------------------------------------------------------------
 * java.util.Random PREDICTABLE hai. Uska seed time se banta hai,
 * to koi banda guess kar sakta hai ki kis time pe kya password bana.
 *
 * SecureRandom OS ke cryptographic source se number leta hai —
 * guess karna practically impossible.
 *
 * Password ke liye HAMESHA SecureRandom.
 *
 * CONFUSING CHARACTERS HATA DIYE
 * ---------------------------------------------------------------------------
 * O aur 0, l aur 1, I aur | — ye mail me padh kar type karte
 * waqt log galti karte hain.
 *
 * Isliye inhe charset se hi nikal diya hai.
 * Thoda kam randomness, lekin bahut kam support calls. 🙂
 * ============================================================================
 */
public final class PasswordGeneratorUtil {

    private PasswordGeneratorUtil() {
        throw new UnsupportedOperationException("Utility class hai, object mat banao");
    }

    /**
     * SecureRandom banana thoda mehnga kaam hai,
     * isliye ek hi baar bana ke reuse kar rahe hain.
     *
     * Ye thread-safe hai, to multiple request ek saath
     * aayein tab bhi problem nahi.
     */
    private static final SecureRandom RANDOM = new SecureRandom();

    // Confusing letters (O, I, l) hata diye hain
    private static final String UPPER   = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String LOWER   = "abcdefghijkmnopqrstuvwxyz";

    // 0 aur 1 hata diye hain
    private static final String DIGITS  = "23456789";

    // Sirf simple symbols — @ # $ % jaise jo mail me theek dikhein
    private static final String SYMBOLS = "@#$%&*";

    private static final String ALL = UPPER + LOWER + DIGITS + SYMBOLS;

    /** Default password length */
    private static final int DEFAULT_LENGTH = 10;


    // ========================================================================
    // MAIN METHOD
    // ========================================================================
    /**
     * Default 10 character ka password banata hai.
     *
     * Service me bas itna:
     *     String plainPassword = PasswordGeneratorUtil.generate();
     */
    public static String generate() {
        return generate(DEFAULT_LENGTH);
    }

    /**
     * Diye gaye length ka password banata hai.
     *
     * GUARANTEE: password me kam se kam ek-ek
     * uppercase, lowercase, digit aur symbol hoga.
     *
     * Ye guarantee kyun zaroori hai?
     * ---------------------------------------------------------------------
     * Agar sirf random chunte to kabhi-kabhi aisa password
     * ban jata jisme sirf lowercase hote (jaise "kmnpqrstuv").
     * Wo kamzor hai aur aage jab password-policy lagayenge
     * to wo apna hi generated password reject kar degi.
     *
     * @param length kam se kam 8
     */
    public static String generate(int length) {

        if (length < 8) {
            throw new IllegalArgumentException("Password kam se kam 8 character ka hona chahiye");
        }

        List<Character> chars = new ArrayList<>(length);

        // Step 1 : har category se ek-ek pakka daal do
        chars.add(pickFrom(UPPER));
        chars.add(pickFrom(LOWER));
        chars.add(pickFrom(DIGITS));
        chars.add(pickFrom(SYMBOLS));

        // Step 2 : baaki jagah kahin se bhi bhar do
        for (int i = 4; i < length; i++) {
            chars.add(pickFrom(ALL));
        }

        /*
         * Step 3 : SHUFFLE
         *
         * Ye step skip mat karna. Bina shuffle ke
         * har password ka pattern fix ho jata —
         * pehla letter hamesha uppercase, chautha hamesha symbol.
         *
         * Pattern pata hone se password guess karna aasan ho jata hai.
         *
         * Collections.shuffle ko SecureRandom pass kar rahe hain,
         * default Random nahi.
         */
        Collections.shuffle(chars, RANDOM);

        StringBuilder sb = new StringBuilder(length);
        for (char c : chars) {
            sb.append(c);
        }

        return sb.toString();
    }


    // ========================================================================
    // EXTRA : Reset Token
    // ========================================================================
    /**
     * "Forgot Password" ke liye random token.
     *
     * Abhi zarurat nahi, lekin migration me humne purane
     * admins ka password 'NEEDS_RESET' set kiya tha —
     * unke liye aage ye kaam aayega.
     *
     * Sirf letters + digits, koi symbol nahi
     * (kyunki ye URL me jayega).
     */
    public static String generateToken(int length) {

        String tokenChars = UPPER + LOWER + DIGITS;
        StringBuilder sb = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            sb.append(pickFrom(tokenChars));
        }

        return sb.toString();
    }


    // ========================================================================
    // HELPER
    // ========================================================================
    /**
     * Di gayi string me se ek random character uthata hai.
     */
    private static char pickFrom(String source) {
        return source.charAt(RANDOM.nextInt(source.length()));
    }
}