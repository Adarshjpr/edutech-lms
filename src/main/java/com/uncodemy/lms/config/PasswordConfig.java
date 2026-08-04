package com.uncodemy.lms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * ============================================================================
 * PasswordConfig
 * ============================================================================
 *
 * Sirf ek kaam : PasswordEncoder ka bean banana.
 *
 * YE SECURITY CONFIG NAHI HAI
 * ---------------------------------------------------------------------------
 * Isse koi endpoint block nahi hoga, koi login page nahi aayega.
 * Ye sirf password ko HASH karne ka tool deti hai.
 *
 * Saare APIs abhi bhi khule rahenge — jaisa plan tha.
 *
 * PLAIN TEXT PASSWORD KYUN NAHI?
 * ---------------------------------------------------------------------------
 * Agar DB me plain password rakha aur kabhi database leak hua,
 * to har trainer/student ka password seedha attacker ke paas.
 *
 * Aur log log same password har jagah use karte hain —
 * to unka Gmail bhi khatre me aa jata hai.
 *
 * BCrypt ke saath leak hone par bhi password nikalna
 * practically impossible hai.
 *
 * BCRYPT KAISE KAAM KARTA HAI
 * ---------------------------------------------------------------------------
 * encode("Kf7@mQx2")
 *   -> $2a$10$N9qo8uLOickgx2ZMRZoMye1J3.QqXxXt5Rk...
 *
 * Ye HASH hai — isse wapas original password nahi nikal sakte.
 *
 * Login ke waqt:
 *   matches("Kf7@mQx2", storedHash)  ->  true / false
 *
 * SALT
 * ---------------------------------------------------------------------------
 * BCrypt har baar alag random "salt" milata hai.
 *
 * Isliye ek hi password encode karo do baar, to
 * do ALAG hash aayenge:
 *
 *   encode("abc123") -> $2a$10$AAA...
 *   encode("abc123") -> $2a$10$BBB...
 *
 * Dono valid hain! matches() dono pe true dega.
 *
 * Fayda: do banda same password rakhein to DB dekh kar
 * pata nahi chalega ki unka password same hai.
 *
 * NOTE: salt hash ke andar hi store hota hai,
 * alag column banane ki zarurat nahi.
 *
 * STRENGTH = 10
 * ---------------------------------------------------------------------------
 * Ye batata hai hash banane me kitni mehnat lagegi.
 *
 *   10 -> ~100ms  (default, LMS ke liye perfect)
 *   12 -> ~400ms
 *   14 -> ~1.5s
 *
 * Zyada strength = attacker ke liye password crack karna
 * utna hi mushkil, lekin apna server bhi slow.
 *
 * 10 industry standard hai.
 *
 * IMPORTANT: column length
 * ---------------------------------------------------------------------------
 * BCrypt hash HAMESHA 60 character ka hota hai.
 * Humne VARCHAR(255) rakha hai — kaafi hai. ✔
 * ============================================================================
 */
@Configuration
public class PasswordConfig {

    /**
     * PasswordEncoder bean.
     *
     * Ab kisi bhi service me bas inject karo:
     *
     *     private final PasswordEncoder passwordEncoder;
     *
     * Aur use karo:
     *
     *     trainer.setPassword(passwordEncoder.encode(plainPassword));
     *
     * INTERFACE return kar rahe hain, class nahi.
     * ---------------------------------------------------------------------
     * Return type PasswordEncoder hai, BCryptPasswordEncoder nahi.
     *
     * Kal ko BCrypt se Argon2 pe jaana ho to sirf yahi
     * ek line badalni padegi — baaki pura project waise ka waisa.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }
}