package cz.previt.bydzovctverec.web;

import cz.previt.bydzovctverec.domain.Edition;
import cz.previt.bydzovctverec.domain.EditionRepository;
import cz.previt.bydzovctverec.domain.MessageLog;
import cz.previt.bydzovctverec.domain.MessageLogRepository;
import cz.previt.bydzovctverec.domain.RacerRegistration;
import cz.previt.bydzovctverec.domain.RacerRegistrationRepository;
import cz.previt.bydzovctverec.domain.AppRole;
import cz.previt.bydzovctverec.domain.AppRoleRepository;
import cz.previt.bydzovctverec.domain.User;
import cz.previt.bydzovctverec.domain.UserRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/notify")
public class AdminNotifyController {

  private static final Logger log = LoggerFactory.getLogger(AdminNotifyController.class);

  private final RacerRegistrationRepository racerRegistrationRepository;
  private final UserRepository userRepository;
  private final EditionRepository editionRepository;
  private final MessageLogRepository messageLogRepository;
  private final JavaMailSender mailSender;
  private final AppRoleRepository appRoleRepository;

  public AdminNotifyController(RacerRegistrationRepository racerRegistrationRepository,
      UserRepository userRepository, EditionRepository editionRepository,
      MessageLogRepository messageLogRepository, JavaMailSender mailSender,
      AppRoleRepository appRoleRepository) {
    this.racerRegistrationRepository = racerRegistrationRepository;
    this.userRepository = userRepository;
    this.editionRepository = editionRepository;
    this.messageLogRepository = messageLogRepository;
    this.mailSender = mailSender;
    this.appRoleRepository = appRoleRepository;
  }

  @GetMapping
  public ResponseEntity<?> history() {
    return ResponseEntity.ok(messageLogRepository.findAllByOrderByCreatedAtDesc());
  }

  @PostMapping
  @Transactional
  public ResponseEntity<?> send(@RequestBody Map<String, String> body) {
    String recipientType = body.get("recipientType");
    String subject = body.get("subject");
    String messageBody = body.get("body");

    if (recipientType == null || recipientType.isBlank()) {
      return ResponseEntity.badRequest().body(Map.of("error", "Chybí typ příjemce"));
    }
    if (subject == null || subject.isBlank()) {
      return ResponseEntity.badRequest().body(Map.of("error", "Chybí předmět"));
    }
    if (messageBody == null || messageBody.isBlank()) {
      return ResponseEntity.badRequest().body(Map.of("error", "Chybí zpráva"));
    }

    List<String> emails = resolveRecipients(recipientType);
    if (emails.isEmpty()) {
      return ResponseEntity.badRequest().body(Map.of("error", "Žádní příjemci"));
    }

    int sent = 0;
    for (String email : emails) {
      try {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(email);
        msg.setSubject(subject);
        msg.setText(messageBody);
        mailSender.send(msg);
        sent++;
      } catch (Exception e) {
        log.warn("Failed to send email to {}: {}", email, e.getMessage());
      }
    }

    messageLogRepository.save(new MessageLog(recipientType, subject, messageBody, sent, Instant.now()));
    log.info("Notified {} / {} recipients (type={})", sent, emails.size(), recipientType);

    return ResponseEntity.ok(Map.of(
        "sent", sent,
        "total", emails.size(),
        "recipientType", recipientType));
  }

  private List<String> resolveRecipients(String type) {
    return switch (type) {
      case "ALL_RACERS" -> {
        Edition edition = editionRepository.findTopByOrderByEditionYearDesc().orElse(null);
        if (edition == null) yield List.of();
        yield racerRegistrationRepository.findByEditionOrderByStartNumber(edition).stream()
            .map(RacerRegistration::getEmail)
            .filter(e -> e != null && !e.isBlank())
            .distinct()
            .toList();
      }
      case "ADMINS" -> {
        var adminRole = appRoleRepository.findByName("ADMIN");
        if (adminRole.isEmpty()) yield List.of();
        yield userRepository.findAll().stream()
            .filter(u -> u.getAppRoles().stream().anyMatch(r -> r.getId().equals(adminRole.get().getId())))
            .map(User::getEmail)
            .filter(e -> e != null && !e.isBlank())
            .toList();
      }
      case "JUDGES" -> {
        var judgeRole = appRoleRepository.findByName("JUDGE");
        if (judgeRole.isEmpty()) yield List.of();
        yield userRepository.findAll().stream()
            .filter(u -> u.getAppRoles().stream().anyMatch(r -> r.getId().equals(judgeRole.get().getId())))
            .map(User::getEmail)
            .filter(e -> e != null && !e.isBlank())
            .toList();
      }
      default -> List.of();
    };
  }
}
