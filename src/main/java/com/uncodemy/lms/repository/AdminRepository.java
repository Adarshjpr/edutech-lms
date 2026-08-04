package com.uncodemy.lms.repository;

import com.uncodemy.lms.model.Admin;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * ============================================================================
 * AdminRepository
 * ============================================================================
 *
 * Admin table ki queries. Chhoti hai — abhi zyada zarurat nahi.
 *
 * Kahan use hogi:
 *   Phase 3 -> approve/reject karte waqt "kisne kiya" save karna
 *   Phase 4 -> student add karne wala admin
 *   Phase 6 -> admin ka announcement
 *   Phase 3 -> trainer ki nayi request ka mail admins ko bhejna
 * ============================================================================
 */
@Repository
public interface AdminRepository extends JpaRepository<Admin, Long> {

    /**
     * Business ID se admin dhundo.
     * Example: findByAdminId("ADM101")
     */
    Optional<Admin> findByAdminId(String adminId);

    /**
     * Email se dhundo (aage login me kaam aayega).
     */
    Optional<Admin> findByEmail(String email);

    boolean existsByAdminId(String adminId);

    boolean existsByEmail(String email);

    /**
     * Last admin ID — IdGeneratorUtil ke liye.
     *
     * Abhi admin create ki API nahi bana rahe (admin
     * seedha DB me daal doge), lekin aage zarurat padegi.
     */
    @Query("SELECT a.adminId FROM Admin a ORDER BY a.id DESC LIMIT 1")
    Optional<String> findLastAdminId();

    /**
     * Saare active admins ke email.
     *
     * KAHAN LAGEGA
     * -----------------------------------------------------------------------
     * Jab trainer koi approval request bheje, to SAARE
     * admins ko mail jani chahiye — taaki jo pehle dekhe
     * wo approve kar de.
     *
     * Sirf ek admin ko bhejte to wo chhutti pe hota to
     * request padi rehti.
     *
     * Poori entity ki jagah sirf email nikal rahe hain.
     */
    @Query("SELECT a.email FROM Admin a WHERE a.active = true")
    List<String> findAllActiveEmails();

    /** Saare active admins */
    List<Admin> findByActiveTrue();
}