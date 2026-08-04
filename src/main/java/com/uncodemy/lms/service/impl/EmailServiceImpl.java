package com.uncodemy.lms.service.impl;

import com.uncodemy.lms.service.rule.EmailService;
import com.uncodemy.lms.util.EmailTemplateBuilder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.*;

import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================================
 * EmailServiceImpl   ---  Amazon SES v2
 * ============================================================================
 *
 * EmailService interface ka asli implementation.
 *
 * @Async
 * ---------------------------------------------------------------------------
 * Har method background thread me chalegi.
 *
 * Matlab TrainerService jab emailService.sendTrainerCredentials()
 * call karegi, to wo line TURANT return kar degi —
 * mail peeche chalti rahegi.
 *
 * User ko response 200ms me mil jayega, 2 second me nahi.
 *
 * ZAROORI: @Async kaam karne ke liye AsyncConfig chahiye
 * (agli file me de raha hoon). Bina uske ye annotation
 * chup-chaap ignore ho jayega.
 *
 * EXCEPTION YAHIN KHATAM HO JATI HAI
 * ---------------------------------------------------------------------------
 * Mail fail ho to exception UPAR NAHI jayegi — sirf log hogi.
 *
 * Kyun? Kyunki mail fail hone se trainer create hona
 * cancel nahi hona chahiye. Trainer ban gaya, bas mail
 * nahi gayi — admin dobara bhej dega.
 *
 * (Waise bhi @Async ki wajah se exception upar ja hi nahi sakti,
 *  kyunki caller to kab ka aage badh chuka hota hai.)
 * ============================================================================
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final SesV2Client sesClient;

    @Value("${app.mail.from}")
    private String fromEmail;

    @Value("${app.mail.from-name}")
    private String fromName;

    @Value("${app.mail.reply-to}")
    private String replyTo;

    @Value("${app.mail.dry-run:false}")
    private boolean dryRun;

    /**
     * SES ki limit : ek request me max 50 recipients.
     * Isse zyada hue to batch me tod denge.
     */
    private static final int BCC_BATCH_SIZE = 45;


    // ========================================================================
    // 1. SINGLE EMAIL  (base method)
    // ========================================================================
    @Async
    @Override
    public void sendEmail(String to, String subject, String htmlBody) {

        if (to == null || to.isBlank()) {
            log.warn("MAIL SKIP : recipient khali hai | subject = {}", subject);
            return;
        }

        // Dev mode : sach me mail nahi bhejenge
        if (dryRun) {
            log.info("MAIL [DRY-RUN] to={} | subject={}", to, subject);
            return;
        }

        try {
            SendEmailRequest request = SendEmailRequest.builder()
                    .fromEmailAddress(formattedFrom())
                    .replyToAddresses(replyTo)
                    .destination(Destination.builder()
                            .toAddresses(to)
                            .build())
                    .content(buildContent(subject, htmlBody))
                    .build();

            SendEmailResponse response = sesClient.sendEmail(request);

            log.info("MAIL SENT | to={} | messageId={}", to, response.messageId());

        } catch (SesV2Exception e) {
            // SES ne mana kiya — sandbox mode, unverified domain, quota khatam
            log.error("MAIL FAILED (SES) | to={} | subject={} | reason={}",
                    to, subject, e.awsErrorDetails().errorMessage());

        } catch (Exception e) {
            log.error("MAIL FAILED | to={} | subject={}", to, subject, e);
        }
    }


    // ========================================================================
    // 2. BULK EMAIL  (API 8 aur 9 ke liye)
    // ========================================================================
    /**
     * BCC KYUN, TO KYUN NAHI?
     * -----------------------------------------------------------------------
     * "to" me 200 students daal dete to har student ko
     * baaki 199 ke email address dikhte. Privacy khatam.
     *
     * BCC me kisi ko koi nahi dikhta.
     *
     * "to" me kya jayega? -> khud ka fromEmail.
     * SES ko kam se kam ek "to" chahiye hota hai.
     *
     * BATCHING
     * -----------------------------------------------------------------------
     * SES ek request me max 50 recipient leta hai.
     * 200 students hain -> 45-45 ke 5 batch banenge.
     *
     * NOTE: ye method @Async NAHI hai.
     * Kyunki isko call karne wali service khud
     * background me chal rahi hogi, aur usse
     * return count chahiye (recipientCount save karne ke liye).
     */
    @Override
    public int sendBulkEmail(List<String> recipients, String subject, String htmlBody) {

        if (recipients == null || recipients.isEmpty()) {
            log.warn("BULK MAIL SKIP : koi recipient nahi | subject = {}", subject);
            return 0;
        }

        // Khali / duplicate email hata do
        List<String> clean = recipients.stream()
                .filter(e -> e != null && !e.isBlank())
                .map(String::trim)
                .distinct()
                .toList();

        if (clean.isEmpty()) {
            return 0;
        }

        if (dryRun) {
            log.info("BULK MAIL [DRY-RUN] | count={} | subject={}", clean.size(), subject);
            return clean.size();
        }

        int sentCount = 0;

        // 45-45 ke chunks me todo
        for (int i = 0; i < clean.size(); i += BCC_BATCH_SIZE) {

            int end = Math.min(i + BCC_BATCH_SIZE, clean.size());
            List<String> chunk = new ArrayList<>(clean.subList(i, end));

            try {
                SendEmailRequest request = SendEmailRequest.builder()
                        .fromEmailAddress(formattedFrom())
                        .replyToAddresses(replyTo)
                        .destination(Destination.builder()
                                .toAddresses(fromEmail)   // dummy "to"
                                .bccAddresses(chunk)      // asli recipients
                                .build())
                        .content(buildContent(subject, htmlBody))
                        .build();

                sesClient.sendEmail(request);
                sentCount += chunk.size();

                log.info("BULK MAIL SENT | chunk={} | total so far={}", chunk.size(), sentCount);

            } catch (SesV2Exception e) {
                // Ek chunk fail hua to baaki chunks band mat karo
                log.error("BULK MAIL CHUNK FAILED | size={} | reason={}",
                        chunk.size(), e.awsErrorDetails().errorMessage());

            } catch (Exception e) {
                log.error("BULK MAIL CHUNK FAILED | size={}", chunk.size(), e);
            }
        }

        log.info("BULK MAIL DONE | requested={} | sent={} | subject={}",
                clean.size(), sentCount, subject);

        return sentCount;
    }


    // ========================================================================
    // 3. API 2 --- Trainer Credentials
    // ========================================================================
    @Async
    @Override
    public void sendTrainerCredentials(String to, String trainerName,
                                       String username, String plainPassword) {

        String subject = "Uncodemy LMS - Aapka Trainer Account Ready Hai";
        String body = EmailTemplateBuilder.trainerCredentials(trainerName, username, plainPassword);

        sendEmailInternal(to, subject, body);
    }


    // ========================================================================
    // 4. API 7 --- Student Welcome
    // ========================================================================
    @Async
    @Override
    public void sendStudentWelcome(String to, String studentName,
                                   String studentId, String batchName) {

        String subject = "Welcome to Uncodemy - " + batchName;
        String body = EmailTemplateBuilder.studentWelcome(studentName, studentId, batchName);

        sendEmailInternal(to, subject, body);
    }


    // ========================================================================
    // 5. API 4 / 7 --- Approval Accepted
    // ========================================================================
    @Async
    @Override
    public void sendApprovalAccepted(String to, String trainerName,
                                     String requestType, String itemName) {

        String subject = "Request Approved - " + requestType;
        String body = EmailTemplateBuilder.approvalAccepted(trainerName, requestType, itemName);

        sendEmailInternal(to, subject, body);
    }


    // ========================================================================
    // 6. API 4 / 7 --- Approval Rejected
    // ========================================================================
    @Async
    @Override
    public void sendApprovalRejected(String to, String trainerName,
                                     String requestType, String itemName, String reason) {

        String subject = "Request Rejected - " + requestType;
        String body = EmailTemplateBuilder.approvalRejected(trainerName, requestType, itemName, reason);

        sendEmailInternal(to, subject, body);
    }


    // ========================================================================
    // 7. Admin ko nayi request ka notification
    // ========================================================================
    @Async
    @Override
    public void notifyAdminNewRequest(String adminEmail, String trainerName,
                                      String requestType, String itemName) {

        String subject = "Nayi Approval Request - " + requestType;
        String body = EmailTemplateBuilder.adminNewRequest(trainerName, requestType, itemName);

        sendEmailInternal(adminEmail, subject, body);
    }


    // ========================================================================
    // PRIVATE HELPERS
    // ========================================================================

    /**
     * Ye internal method isliye hai kyunki upar wale
     * @Async methods se sendEmail() ko direct call
     * karna kaam nahi karta.
     *
     * KYUN?
     * -----------------------------------------------------------------------
     * @Async Spring ke PROXY se chalta hai. Jab class apne hi
     * andar ka method call karti hai (this.sendEmail), to proxy
     * beech me aata hi nahi — matlab @Async ignore ho jata hai.
     *
     * Ye Spring ka bahut famous "self-invocation" problem hai.
     *
     * Yahan farak nahi padta kyunki calling method KHUD
     * already async hai. Lekin confusion se bachne ke liye
     * alag private method bana diya.
     */
    private void sendEmailInternal(String to, String subject, String htmlBody) {

        if (to == null || to.isBlank()) {
            log.warn("MAIL SKIP : recipient khali | subject={}", subject);
            return;
        }

        if (dryRun) {
            log.info("MAIL [DRY-RUN] to={} | subject={}", to, subject);
            return;
        }

        try {
            SendEmailRequest request = SendEmailRequest.builder()
                    .fromEmailAddress(formattedFrom())
                    .replyToAddresses(replyTo)
                    .destination(Destination.builder().toAddresses(to).build())
                    .content(buildContent(subject, htmlBody))
                    .build();

            SendEmailResponse response = sesClient.sendEmail(request);
            log.info("MAIL SENT | to={} | messageId={}", to, response.messageId());

        } catch (SesV2Exception e) {
            log.error("MAIL FAILED (SES) | to={} | reason={}",
                    to, e.awsErrorDetails().errorMessage());

        } catch (Exception e) {
            log.error("MAIL FAILED | to={}", to, e);
        }
    }

    /**
     * "Uncodemy LMS <no-reply@uncodemy.com>"
     *
     * Aise likhne se inbox me sender ka naam
     * dikhta hai, raw email address nahi.
     */
    private String formattedFrom() {
        return String.format("%s <%s>", fromName, fromEmail);
    }

    /**
     * Subject + HTML body ko SES ke format me daalta hai.
     *
     * UTF-8 zaroori hai — warna Hindi text ya emoji
     * mail me tooti hui dikhegi.
     */
    private EmailContent buildContent(String subject, String htmlBody) {

        return EmailContent.builder()
                .simple(Message.builder()
                        .subject(Content.builder()
                                .data(subject)
                                .charset("UTF-8")
                                .build())
                        .body(Body.builder()
                                .html(Content.builder()
                                        .data(htmlBody)
                                        .charset("UTF-8")
                                        .build())
                                .build())
                        .build())
                .build();
    }
}