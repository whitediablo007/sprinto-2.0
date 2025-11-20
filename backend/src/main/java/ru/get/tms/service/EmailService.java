package ru.get.tms.service;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Service for sending emails via SMTP.
 *
 * <p>Uses JavaMailSender for SMTP communication and Thymeleaf for email templates. All email
 * operations are executed on boundedElastic scheduler to avoid blocking the reactive pipeline
 * (blocking I/O isolation per Constitution Principle I).
 *
 * <p>Supported email types:
 *
 * <ul>
 *   <li>Password reset emails (FR-007.1)
 *   <li>Welcome emails (user registration)
 *   <li>Notification emails (configurable per user preferences)
 * </ul>
 *
 * @see ru.get.tms.service.PasswordResetService
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

  private final JavaMailSender mailSender;
  private final TemplateEngine templateEngine;

  @Value("${spring.mail.from}")
  private String fromEmail;

  @Value("${app.frontend.url}")
  private String frontendUrl;

  /**
   * Send password reset email with token.
   *
   * @param toEmail recipient email
   * @param userName recipient name
   * @param resetToken password reset token
   * @return Mono&lt;Void&gt; that completes when email is sent
   */
  public Mono<Void> sendPasswordResetEmail(String toEmail, String userName, String resetToken) {
    log.info("Sending password reset email to: {}", toEmail);

    return Mono.fromRunnable(
            () -> {
              try {
                var mimeMessage = mailSender.createMimeMessage();
                var helper = new MimeMessageHelper(mimeMessage, StandardCharsets.UTF_8.name());

                helper.setFrom(fromEmail);
                helper.setTo(toEmail);
                helper.setSubject("Восстановление пароля - Team Task Manager");

                String resetUrl = frontendUrl + "/auth/reset-password?token=" + resetToken;
                Map<String, Object> variables =
                    Map.of("userName", userName, "resetUrl", resetUrl, "validityHours", 1);

                String htmlContent = buildEmailContent("password-reset", variables);
                helper.setText(htmlContent, true);

                mailSender.send(mimeMessage);
                log.info("Password reset email sent successfully to: {}", toEmail);
              } catch (Exception e) {
                log.error("Failed to send password reset email to: {}", toEmail, e);
                throw new RuntimeException("Failed to send email", e);
              }
            })
        .subscribeOn(Schedulers.boundedElastic()) // Blocking I/O isolation
        .then();
  }

  /**
   * Send welcome email to newly registered user.
   *
   * @param toEmail recipient email
   * @param userName recipient name
   * @return Mono&lt;Void&gt; that completes when email is sent
   */
  public Mono<Void> sendWelcomeEmail(String toEmail, String userName) {
    log.info("Sending welcome email to: {}", toEmail);

    return Mono.fromRunnable(
            () -> {
              try {
                var mimeMessage = mailSender.createMimeMessage();
                var helper = new MimeMessageHelper(mimeMessage, StandardCharsets.UTF_8.name());

                helper.setFrom(fromEmail);
                helper.setTo(toEmail);
                helper.setSubject("Добро пожаловать в Team Task Manager!");

                Map<String, Object> variables =
                    Map.of("userName", userName, "loginUrl", frontendUrl + "/auth/login");

                String htmlContent = buildEmailContent("welcome", variables);
                helper.setText(htmlContent, true);

                mailSender.send(mimeMessage);
                log.info("Welcome email sent successfully to: {}", toEmail);
              } catch (Exception e) {
                log.error("Failed to send welcome email to: {}", toEmail, e);
                throw new RuntimeException("Failed to send email", e);
              }
            })
        .subscribeOn(Schedulers.boundedElastic())
        .then();
  }

  /**
   * Build email content from Thymeleaf template.
   *
   * @param templateName template name (without extension)
   * @param variables template variables
   * @return rendered HTML content
   */
  private String buildEmailContent(String templateName, Map<String, Object> variables) {
    Context context = new Context();
    context.setVariables(variables);
    return templateEngine.process("email/" + templateName, context);
  }
}
