package com.uncodemy.lms.util;

/**
 * ============================================================================
 * EmailTemplateBuilder
 * ============================================================================
 *
 * Saare mails ka HTML yahan banta hai.
 *
 * MODERN CSS KYUN NAHI USE KIYA?
 * ---------------------------------------------------------------------------
 * Email clients (Outlook, Gmail, Yahoo) browser jaisa
 * HTML render nahi karte.
 *
 *   ✘ flexbox / grid      -> kaam nahi karta
 *   ✘ <style> tag          -> Gmail hata deta hai
 *   ✘ position, float      -> tootta hai
 *
 *   ✔ <table>              -> har jagah kaam karta hai
 *   ✔ inline style=""      -> yahi safe hai
 *
 * Isliye ye code purane zamane ka lagta hai — lekin
 * yahi tareeka har client me sahi dikhta hai.
 *
 * THYMELEAF KYUN NAHI?
 * ---------------------------------------------------------------------------
 * Sirf 7 template ke liye poori templating library
 * add karna zyada hai. Plain Java se kaam ho jayega.
 *
 * Aage 20-30 template ho jayein to Thymeleaf pe shift kar lenge —
 * sirf ye ek class badalni padegi, baaki project waisa ka waisa.
 * ============================================================================
 */
public final class EmailTemplateBuilder {

    private EmailTemplateBuilder() {
        throw new UnsupportedOperationException("Utility class hai, object mat banao");
    }

    // Brand colors — ek jagah, taaki sab jagah same rahe
    private static final String BRAND      = "#1a56db";
    private static final String SUCCESS    = "#0e9f6e";
    private static final String DANGER     = "#e02424";
    private static final String TEXT_DARK  = "#111827";
    private static final String TEXT_LIGHT = "#6b7280";
    private static final String BG         = "#f3f4f6";

    private static final String COMPANY = "Uncodemy";


    // ========================================================================
    // API 2 --- TRAINER CREDENTIALS
    // ========================================================================
    public static String trainerCredentials(String trainerName, String username,
                                            String plainPassword) {

        String body = """
                <p style="%s">Namaste <b>%s</b>,</p>

                <p style="%s">
                    Uncodemy LMS par aapka Trainer account bana diya gaya hai.
                    Neeche diye gaye credentials se aap login kar sakte hain.
                </p>

                <table width="100%%" cellpadding="0" cellspacing="0"
                       style="background:%s;border-radius:6px;margin:20px 0;">
                    <tr>
                        <td style="padding:18px 20px;">
                            <p style="margin:0 0 10px;font-size:13px;color:%s;">USERNAME</p>
                            <p style="margin:0 0 18px;font-size:17px;font-weight:bold;color:%s;
                                      font-family:monospace;">%s</p>

                            <p style="margin:0 0 10px;font-size:13px;color:%s;">PASSWORD</p>
                            <p style="margin:0;font-size:17px;font-weight:bold;color:%s;
                                      font-family:monospace;letter-spacing:1px;">%s</p>
                        </td>
                    </tr>
                </table>

                <table width="100%%" cellpadding="0" cellspacing="0"
                       style="background:#fff8e1;border-left:4px solid #f59e0b;border-radius:4px;">
                    <tr>
                        <td style="padding:14px 16px;">
                            <p style="margin:0;font-size:14px;color:#92400e;">
                                <b>Zaroori:</b> Pehli baar login karne ke baad
                                apna password turant change kar lein.
                                Ye password kisi ke saath share na karein.
                            </p>
                        </td>
                    </tr>
                </table>
                """.formatted(
                pStyle(), esc(trainerName),
                pStyle(),
                BG,
                TEXT_LIGHT, TEXT_DARK, esc(username),
                TEXT_LIGHT, BRAND, esc(plainPassword)
        );

        return wrap("Trainer Account Ready", BRAND, body);
    }


    // ========================================================================
    // API 7 --- STUDENT WELCOME
    // ========================================================================
    public static String studentWelcome(String studentName, String studentId,
                                        String batchName) {

        String body = """
                <p style="%s">Namaste <b>%s</b>,</p>

                <p style="%s">
                    Uncodemy me aapka swagat hai! Aapko batch
                    <b>%s</b> me successfully enroll kar diya gaya hai.
                </p>

                %s

                <p style="%s">
                    Class ki timing, study material aur announcements
                    aapko isi email par milte rahenge.
                </p>

                <p style="%s">Happy Learning! 🎓</p>
                """.formatted(
                pStyle(), esc(studentName),
                pStyle(), esc(batchName),
                infoBox(new String[][]{
                        {"STUDENT ID", esc(studentId)},
                        {"BATCH", esc(batchName)}
                }),
                pStyle(),
                pStyle()
        );

        return wrap("Welcome to " + COMPANY, SUCCESS, body);
    }


    // ========================================================================
    // API 4 / 7 --- APPROVAL ACCEPTED
    // ========================================================================
    public static String approvalAccepted(String trainerName, String requestType,
                                          String itemName) {

        String body = """
                <p style="%s">Namaste <b>%s</b>,</p>

                <p style="%s">
                    Aapki request Admin dwara <b style="color:%s;">APPROVE</b>
                    kar di gayi hai.
                </p>

                %s

                <p style="%s">
                    Ab aap ise normally use kar sakte hain.
                </p>
                """.formatted(
                pStyle(), esc(trainerName),
                pStyle(), SUCCESS,
                infoBox(new String[][]{
                        {"REQUEST TYPE", esc(requestType)},
                        {"DETAILS", esc(itemName)},
                        {"STATUS", "APPROVED"}
                }),
                pStyle()
        );

        return wrap("Request Approved", SUCCESS, body);
    }


    // ========================================================================
    // API 4 / 7 --- APPROVAL REJECTED
    // ========================================================================
    public static String approvalRejected(String trainerName, String requestType,
                                          String itemName, String reason) {

        String body = """
                <p style="%s">Namaste <b>%s</b>,</p>

                <p style="%s">
                    Aapki request Admin dwara <b style="color:%s;">REJECT</b>
                    kar di gayi hai.
                </p>

                %s

                <table width="100%%" cellpadding="0" cellspacing="0"
                       style="background:#fef2f2;border-left:4px solid %s;border-radius:4px;margin:18px 0;">
                    <tr>
                        <td style="padding:14px 16px;">
                            <p style="margin:0 0 6px;font-size:13px;color:%s;">REASON</p>
                            <p style="margin:0;font-size:15px;color:%s;">%s</p>
                        </td>
                    </tr>
                </table>

                <p style="%s">
                    Koi confusion ho to Admin se sampark karein.
                </p>
                """.formatted(
                pStyle(), esc(trainerName),
                pStyle(), DANGER,
                infoBox(new String[][]{
                        {"REQUEST TYPE", esc(requestType)},
                        {"DETAILS", esc(itemName)}
                }),
                DANGER, TEXT_LIGHT, TEXT_DARK,
                reason == null || reason.isBlank() ? "Koi reason nahi diya gaya" : esc(reason),
                pStyle()
        );

        return wrap("Request Rejected", DANGER, body);
    }


    // ========================================================================
    // ADMIN --- NAYI REQUEST AAYI
    // ========================================================================
    public static String adminNewRequest(String trainerName, String requestType,
                                         String itemName) {

        String body = """
                <p style="%s">Namaste Admin,</p>

                <p style="%s">
                    Trainer <b>%s</b> ne ek nayi request bheji hai
                    jo aapke approval ka intezaar kar rahi hai.
                </p>

                %s

                <p style="%s">
                    Approve ya reject karne ke liye Admin dashboard kholein.
                </p>
                """.formatted(
                pStyle(),
                pStyle(), esc(trainerName),
                infoBox(new String[][]{
                        {"TRAINER", esc(trainerName)},
                        {"REQUEST TYPE", esc(requestType)},
                        {"DETAILS", esc(itemName)},
                        {"STATUS", "PENDING"}
                }),
                pStyle()
        );

        return wrap("Nayi Approval Request", "#f59e0b", body);
    }


    // ========================================================================
    // API 8 --- ANNOUNCEMENT  (Phase 6 me use hoga)
    // ========================================================================
    /**
     * @param batchName null ho sakta hai (GLOBAL announcement ke case me)
     */
    public static String announcement(String title, String message,
                                      String senderName, String batchName) {

        String batchLine = (batchName == null || batchName.isBlank())
                ? ""
                : "<p style=\"margin:0 0 16px;font-size:13px;color:" + TEXT_LIGHT + ";\">"
                  + "Batch : <b>" + esc(batchName) + "</b></p>";

        String body = """
                %s

                <h2 style="margin:0 0 14px;font-size:19px;color:%s;">%s</h2>

                <table width="100%%" cellpadding="0" cellspacing="0"
                       style="background:%s;border-radius:6px;">
                    <tr>
                        <td style="padding:18px 20px;">
                            <p style="margin:0;font-size:15px;line-height:1.7;color:%s;
                                      white-space:pre-line;">%s</p>
                        </td>
                    </tr>
                </table>

                <p style="margin:20px 0 0;font-size:13px;color:%s;">
                    — %s
                </p>
                """.formatted(
                batchLine,
                TEXT_DARK, esc(title),
                BG,
                TEXT_DARK, esc(message),
                TEXT_LIGHT, esc(senderName)
        );

        return wrap("Announcement", BRAND, body);
    }


    // ========================================================================
    // API 9 --- NAYA CONTENT UPLOAD  (Phase 7 me use hoga)
    // ========================================================================
    public static String contentUploaded(String contentTitle, String contentType,
                                         String batchName, String link,
                                         String uploaderName) {

        String body = """
                <p style="%s">
                    Aapke batch <b>%s</b> me naya study material upload hua hai.
                </p>

                %s

                <table cellpadding="0" cellspacing="0" style="margin:22px 0;">
                    <tr>
                        <td style="background:%s;border-radius:6px;">
                            <a href="%s"
                               style="display:inline-block;padding:13px 30px;color:#ffffff;
                                      text-decoration:none;font-size:15px;font-weight:bold;">
                                Material Kholein
                            </a>
                        </td>
                    </tr>
                </table>

                <p style="margin:0;font-size:13px;color:%s;">
                    Upload by : %s
                </p>
                """.formatted(
                pStyle(), esc(batchName),
                infoBox(new String[][]{
                        {"TITLE", esc(contentTitle)},
                        {"TYPE", esc(contentType)},
                        {"BATCH", esc(batchName)}
                }),
                BRAND, esc(link),
                TEXT_LIGHT, esc(uploaderName)
        );

        return wrap("Naya Study Material", BRAND, body);
    }


    // ========================================================================
    // PRIVATE HELPERS
    // ========================================================================

    /**
     * Har mail ka common dhancha — header, body, footer.
     *
     * Sirf beech ka content badalta hai, baaki sab same.
     * Isse design ek jagah maintain hota hai.
     */
    private static String wrap(String headerText, String headerColor, String innerHtml) {

        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>%s</title>
                </head>
                <body style="margin:0;padding:0;background:%s;
                             font-family:Arial,Helvetica,sans-serif;">

                <table width="100%%" cellpadding="0" cellspacing="0" style="background:%s;padding:30px 12px;">
                    <tr>
                        <td align="center">

                            <table width="600" cellpadding="0" cellspacing="0"
                                   style="max-width:600px;background:#ffffff;border-radius:8px;
                                          overflow:hidden;box-shadow:0 1px 3px rgba(0,0,0,0.1);">

                                <!-- HEADER -->
                                <tr>
                                    <td style="background:%s;padding:26px 30px;">
                                        <p style="margin:0;color:#ffffff;font-size:20px;font-weight:bold;">
                                            %s
                                        </p>
                                        <p style="margin:5px 0 0;color:rgba(255,255,255,0.85);font-size:13px;">
                                            %s LMS
                                        </p>
                                    </td>
                                </tr>

                                <!-- BODY -->
                                <tr>
                                    <td style="padding:30px;">
                                        %s
                                    </td>
                                </tr>

                                <!-- FOOTER -->
                                <tr>
                                    <td style="background:#fafafa;padding:20px 30px;
                                               border-top:1px solid #e5e7eb;">
                                        <p style="margin:0;font-size:12px;color:%s;line-height:1.6;">
                                            Ye ek automated email hai, iska reply na karein.<br>
                                            &copy; %s. All rights reserved.
                                        </p>
                                    </td>
                                </tr>

                            </table>

                        </td>
                    </tr>
                </table>

                </body>
                </html>
                """.formatted(
                esc(headerText), BG, BG,
                headerColor, esc(headerText), COMPANY,
                innerHtml,
                TEXT_LIGHT, COMPANY
        );
    }

    /**
     * Grey box jisme label-value pairs dikhte hain.
     *
     * Input: {{"BATCH", "Java Full Stack"}, {"STATUS", "APPROVED"}}
     */
    private static String infoBox(String[][] rows) {

        StringBuilder sb = new StringBuilder();

        sb.append("<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:")
          .append(BG).append(";border-radius:6px;margin:18px 0;\"><tr><td style=\"padding:16px 20px;\">");

        for (int i = 0; i < rows.length; i++) {
            String marginBottom = (i == rows.length - 1) ? "0" : "14px";

            sb.append("<p style=\"margin:0 0 4px;font-size:12px;color:").append(TEXT_LIGHT)
              .append(";letter-spacing:0.5px;\">").append(rows[i][0]).append("</p>")
              .append("<p style=\"margin:0 0 ").append(marginBottom)
              .append(";font-size:15px;font-weight:bold;color:").append(TEXT_DARK)
              .append(";\">").append(rows[i][1]).append("</p>");
        }

        sb.append("</td></tr></table>");
        return sb.toString();
    }

    /** Normal paragraph ka style */
    private static String pStyle() {
        return "margin:0 0 14px;font-size:15px;line-height:1.6;color:" + TEXT_DARK + ";";
    }

    /**
     * HTML ESCAPING --- ye method skip mat karna.
     *
     * PROBLEM
     * -----------------------------------------------------------------------
     * Batch ka naam agar "Java & C++ <Basics>" hai, to seedha
     * HTML me daalne se mail ka layout toot jayega.
     *
     * Aur agar koi jaan-boojh kar naam me <script> daal de,
     * to wo bhi mail me chala jayega.
     *
     * Isliye user ka har input pehle escape hota hai.
     * Announcement ka message bhi user ka hi likha hua hai —
     * wo bhi escape ho raha hai.
     *
     * NOTE: announcement template me "white-space:pre-line" laga hai,
     * isliye escape karne ke baad bhi user ke enter (line break)
     * mail me dikhte hain.
     */
    private static String esc(String input) {

        if (input == null) {
            return "";
        }

        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}