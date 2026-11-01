package com.bank.authservice.service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:${spring.mail.username:}}")
    private String fromEmail;

    @Value("${app.mail.from-name:ApexBank Security}")
    private String fromName;

    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'").withZone(ZoneOffset.UTC);

    public void sendRegistrationOtp(String email, String otp) {
        sendHtmlEmail(
            email,
            "ApexBank — Verify Your Email",
            "Email Verification",
            "Thank you for registering with ApexBank. Use the verification code below to confirm your email address and complete your registration.",
            otp
        );
    }

    public void sendLoginOtp(String email, String otp) {
        sendHtmlEmail(
            email,
            "ApexBank — Your Login Verification Code",
            "Login Verification",
            "A login attempt was initiated for your ApexBank account. Use the one-time passcode below to complete your authentication.",
            otp
        );
    }

    public void sendPasswordResetOtp(String email, String otp) {
        sendHtmlEmail(
            email,
            "ApexBank — Password Reset Verification",
            "Password Reset Verification",
            "A password reset request was received for your ApexBank account. Use the verification code below to authorize your password change.",
            otp
        );
    }

    /**
     * Backward-compatible method for existing callers.
     */
    public void sendOtp(String email, String otp) {
        sendLoginOtp(email, otp);
    }

    private void sendHtmlEmail(String toEmail, String subject, String purposeTitle, String descriptionText, String otp) {
        if (toEmail == null || toEmail.trim().isEmpty()) {
            log.warn("Email OTP dispatch skipped: recipient email is missing");
            throw new IllegalArgumentException("Recipient email address cannot be empty");
        }

        String recipient = toEmail.trim();
        String requestTimestamp = TIMESTAMP_FORMATTER.format(Instant.now());
        String htmlBody = buildHtmlTemplate(purposeTitle, descriptionText, otp, requestTimestamp);

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            String effectiveFrom = (fromEmail != null && !fromEmail.trim().isEmpty()) ? fromEmail.trim() : "no-reply@apexbank.com";
            helper.setFrom(new InternetAddress(effectiveFrom, fromName));
            helper.setTo(recipient);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            mailSender.send(mimeMessage);
            log.info("ApexBank security email [{}] dispatched successfully to masked destination {}", purposeTitle, mask(recipient));
        } catch (Exception ex) {
            log.error("Failed to deliver security email [{}] to masked destination {}: {}", purposeTitle, mask(recipient), ex.getMessage());
            throw new IllegalStateException("Failed to deliver verification email. Please verify your email settings or try again later.", ex);
        }
    }

    private String buildHtmlTemplate(String purposeTitle, String descriptionText, String otp, String timestamp) {
        return "<!DOCTYPE html>"
            + "<html lang=\"en\">"
            + "<head>"
            + "<meta charset=\"UTF-8\">"
            + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">"
            + "<title>" + purposeTitle + "</title>"
            + "<style>"
            + "  body { margin:0; padding:0; background-color:#f1f5f9; font-family:-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; color:#0f172a; }"
            + "  .container { max-width:560px; margin:32px auto; background:#ffffff; border-radius:16px; overflow:hidden; border:1px solid #e2e8f0; box-shadow:0 10px 25px -5px rgba(15, 23, 42, 0.08); }"
            + "  .header { background:linear-gradient(135deg, #0f172a 0%, #1e293b 100%); padding:28px 32px; text-align:center; }"
            + "  .brand { display:inline-block; font-size:22px; font-weight:800; letter-spacing:1px; color:#38bdf8; text-decoration:none; }"
            + "  .brand span { color:#f8fafc; }"
            + "  .badge { display:inline-block; margin-top:8px; padding:4px 12px; background:rgba(56, 189, 248, 0.15); border:1px solid rgba(56, 189, 248, 0.3); border-radius:9999px; color:#38bdf8; font-size:12px; font-weight:600; text-transform:uppercase; letter-spacing:0.5px; }"
            + "  .content { padding:36px 32px; }"
            + "  .title { margin:0 0 12px 0; font-size:20px; font-weight:700; color:#0f172a; }"
            + "  .desc { margin:0 0 24px 0; font-size:14px; line-height:1.6; color:#475569; }"
            + "  .otp-box { background:#f8fafc; border:2px dashed #0284c7; border-radius:12px; padding:20px; text-align:center; margin:24px 0; }"
            + "  .otp-code { font-family:'Courier New', Courier, monospace; font-size:36px; font-weight:800; letter-spacing:10px; color:#0284c7; margin:0; padding-left:10px; }"
            + "  .otp-expiry { margin:8px 0 0 0; font-size:13px; font-weight:600; color:#0369a1; }"
            + "  .warning-box { background:#fffbeb; border-left:4px solid #f59e0b; padding:14px 16px; border-radius:0 8px 8px 0; margin:24px 0; }"
            + "  .warning-title { font-size:12px; font-weight:700; color:#b45309; text-transform:uppercase; margin:0 0 4px 0; }"
            + "  .warning-text { font-size:13px; line-height:1.5; color:#78350f; margin:0; }"
            + "  .meta { margin-top:28px; padding-top:20px; border-top:1px solid #f1f5f9; font-size:12px; color:#94a3b8; display:flex; justify-content:space-between; }"
            + "  .footer { background:#f8fafc; padding:20px 32px; text-align:center; border-top:1px solid #e2e8f0; font-size:12px; color:#64748b; line-height:1.5; }"
            + "</style>"
            + "</head>"
            + "<body>"
            + "  <div class=\"container\">"
            + "    <div class=\"header\">"
            + "      <div class=\"brand\">APEX<span>BANK</span></div><br/>"
            + "      <span class=\"badge\">Security Notification</span>"
            + "    </div>"
            + "    <div class=\"content\">"
            + "      <h1 class=\"title\">" + purposeTitle + "</h1>"
            + "      <p class=\"desc\">" + descriptionText + "</p>"
            + "      <div class=\"otp-box\">"
            + "        <div class=\"otp-code\">" + otp + "</div>"
            + "        <div class=\"otp-expiry\">⏱ Expires in 5 minutes</div>"
            + "      </div>"
            + "      <div class=\"warning-box\">"
            + "        <div class=\"warning-title\">Security Notice</div>"
            + "        <div class=\"warning-text\">Never share this One-Time Passcode (OTP) with anyone. ApexBank representatives will never contact you asking for your verification code or password.</div>"
            + "      </div>"
            + "      <div class=\"meta\">"
            + "        <span>Request Time: " + timestamp + "</span>"
            + "      </div>"
            + "    </div>"
            + "    <div class=\"footer\">"
            + "      &copy; 2026 ApexBank Inc. All rights reserved.<br/>"
            + "      This is an automated security transmission. Please do not reply directly to this email."
            + "    </div>"
            + "  </div>"
            + "</body>"
            + "</html>";
    }

    private String mask(String email) {
        if (email == null) return "***";
        int at = email.indexOf('@');
        if (at <= 1) return "***";
        String user = email.substring(0, at);
        String domain = email.substring(at);
        String maskedUser = user.length() <= 2 ? user.charAt(0) + "***" : user.substring(0, 2) + "****";
        return maskedUser + domain;
    }
}
