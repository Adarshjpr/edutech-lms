package com.uncodemy.lms.service.rule;

import java.util.List;

/**
 * ============================================================================
 * EmailService  (Interface)
 * ============================================================================
 *
 * Project ki saari mail isi ke through jayegi.
 *
 * INTERFACE KYUN BANAYA?
 * ---------------------------------------------------------------------------
 * Baaki service classes (TrainerService, AnnouncementService)
 * sirf iss INTERFACE ko jaanengi — SES ka naam tak nahi.
 *
 * Fayda:
 *   ✔ Kal SES se SendGrid pe jaana ho -> sirf ek nayi impl class
 *   ✔ Test me FakeEmailService laga sakte hain (asli mail nahi jayegi)
 *   ✔ Baaki pura project waise ka waisa
 *
 * SAARI METHODS ASYNC HAIN
 * ---------------------------------------------------------------------------
 * Mail bhejne me 1-2 second lagta hai. Agar 200 students ko
 * announcement ja rahi hai to 3-4 minute!
 *
 * User itni der wait nahi karega. Isliye:
 *
 *   Controller -> Service -> DB me save -> mail ka kaam
 *                                          BACKGROUND me daal do
 *                                          -> turant response
 *
 * Matlab in methods ko call karne wala WAIT NAHI KAREGA.
 * Method turant return kar degi, mail peeche chalti rahegi.
 *
 * ISI WAJAH SE ye methods void hain ya CompletableFuture.
 * ============================================================================
 */
public interface EmailService {

    // ========================================================================
    // 1. GENERIC (base method — baaki sab isi ko call karti hain)
    // ========================================================================

    /**
     * Ek banda, ek mail.
     *
     * @param to       kiske paas
     * @param subject  mail ka subject
     * @param htmlBody HTML body
     */
    void sendEmail(String to, String subject, String htmlBody);


    /**
     * Bahut saare logo ko EK SAATH same mail.
     *
     * API 8 (announcement) aur API 9 (content) me lagegi.
     *
     * BCC use hoga — taaki ek student ko baaki
     * students ke email address na dikhein (privacy).
     *
     * @return kitne logo ko successfully gayi
     */
    int sendBulkEmail(List<String> recipients, String subject, String htmlBody);


    // ========================================================================
    // 2. API 2 --- Trainer ko credentials
    // ========================================================================

    /**
     * Naya trainer bana to usko username + password mail karo.
     *
     * DHYAN DO: plainPassword yahan PLAIN aata hai —
     * ye poore project me sirf yahi jagah hai jahan
     * plain password use hota hai. DB me hash hi jata hai.
     */
    void sendTrainerCredentials(String to, String trainerName,
                                String username, String plainPassword);


    // ========================================================================
    // 3. API 7 --- Student welcome
    // ========================================================================

    /**
     * Student add hone par welcome mail + batch details.
     *
     * NOTE: Trainer ne add kiya to ye mail
     * TABHI jayegi jab admin APPROVE karega.
     * PENDING student ko koi mail nahi.
     */
    void sendStudentWelcome(String to, String studentName,
                            String studentId, String batchName);


    // ========================================================================
    // 4. API 4 / 7 --- Approval ka natija Trainer ko
    // ========================================================================

    /**
     * Admin ne trainer ki request APPROVE kar di.
     *
     * @param requestType "Batch Creation" ya "Student Addition"
     * @param itemName    batch ka naam ya student ka naam
     */
    void sendApprovalAccepted(String to, String trainerName,
                              String requestType, String itemName);

    /**
     * Admin ne REJECT kar di.
     *
     * @param reason admin ka remark — trainer ko pata chalna chahiye kyun.
     */
    void sendApprovalRejected(String to, String trainerName,
                              String requestType, String itemName, String reason);


    /**
     * Trainer ne nayi request bheji — Admin ko batao.
     *
     * Warna admin ko roz dashboard kholna padega
     * ye dekhne ke liye ki koi pending hai ya nahi.
     */
    void notifyAdminNewRequest(String adminEmail, String trainerName,
                               String requestType, String itemName);
}