package com.bank.authservice.service;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Field;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RealEmailDeliveryVerificationTest {

    @Test
    void testRealGmailSmtpDelivery() {
        Properties envProps = loadLocalEnvFile();

        String host = getProperty(envProps, "MAIL_HOST", "smtp.gmail.com");
        String portStr = getProperty(envProps, "MAIL_PORT", "587");
        int port = Integer.parseInt(portStr);
        String username = getProperty(envProps, "MAIL_USERNAME", null);
        String password = getProperty(envProps, "MAIL_PASSWORD", null);
        String from = getProperty(envProps, "MAIL_FROM", username);
        String fromName = getProperty(envProps, "MAIL_FROM_NAME", "ApexBank Security");

        assertNotNull(username, "MAIL_USERNAME must be configured in environment or .env");
        assertNotNull(password, "MAIL_PASSWORD must be configured in environment or .env");

        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(host);
        mailSender.setPort(port);
        mailSender.setUsername(username);
        mailSender.setPassword(password);

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");

        EmailService emailService = new EmailService(mailSender);
        setField(emailService, "fromEmail", from);
        setField(emailService, "fromName", fromName);

        // Send real verification email to the user's configured Gmail inbox
        assertDoesNotThrow(() -> {
            emailService.sendLoginOtp(username, "789012");
        }, "Real email OTP delivery via Gmail SMTP must succeed without throwing exception");
    }

    private String getProperty(Properties props, String key, String defaultValue) {
        String sysEnv = System.getenv(key);
        if (sysEnv != null && !sysEnv.trim().isEmpty()) {
            return sysEnv.trim();
        }
        String fromFile = props.getProperty(key);
        if (fromFile != null && !fromFile.trim().isEmpty()) {
            return fromFile.trim();
        }
        return defaultValue;
    }

    private Properties loadLocalEnvFile() {
        Properties props = new Properties();
        File envFile = new File("../.env");
        if (!envFile.exists()) {
            envFile = new File(".env");
        }
        if (envFile.exists()) {
            try (FileInputStream fis = new FileInputStream(envFile)) {
                Properties raw = new Properties();
                raw.load(fis);
                for (String name : raw.stringPropertyNames()) {
                    String val = raw.getProperty(name);
                    if (val != null) {
                        props.setProperty(name.trim(), val.trim());
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return props;
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
