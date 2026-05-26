package cz.previt.bydzovctverec.web;

import cz.previt.bydzovctverec.domain.Edition;
import cz.previt.bydzovctverec.domain.EditionRepository;
import cz.previt.bydzovctverec.domain.MessageLog;
import cz.previt.bydzovctverec.domain.MessageLogRepository;
import cz.previt.bydzovctverec.domain.Notification;
import cz.previt.bydzovctverec.domain.NotificationRepository;
import cz.previt.bydzovctverec.domain.RacerRegistration;
import cz.previt.bydzovctverec.domain.RacerRegistrationRepository;
import cz.previt.bydzovctverec.domain.AppRole;
import cz.previt.bydzovctverec.domain.AppRoleRepository;
import cz.previt.bydzovctverec.domain.User;
import cz.previt.bydzovctverec.domain.UserRepository;
import cz.previt.bydzovctverec.domain.UserRole;
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
import org.springframework.web.bind.annotation.PathVariable;
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
  private final NotificationRepository notificationRepository;
  private final JavaMailSender mailSender;
  private final AppRoleRepository appRoleRepository;

  public AdminNotifyController(RacerRegistrationRepository racerRegistrationRepository,
      UserRepository userRepository, EditionRepository editionRepository,
      MessageLogRepository messageLogRepository,
      NotificationRepository notificationRepository,
      JavaMailSender mailSender,
      AppRoleRepository appRoleRepository) {
    this.racerRegistrationRepository = racerRegistrationRepository;
    this.userRepository = userRepository;
    this.editionRepository = editionRepository;
    this.messageLogRepository = messageLogRepository;
    this.notificationRepository = notificationRepository;
    this.mailSender = mailSender;
    this.appRoleRepository = appRoleRepository;
  }

  @GetMapping
  public ResponseEntity<?> history() {
    return ResponseEntity.ok(messageLogRepository.findAllByOrderByCreatedAtDesc());
  }

  @GetMapping("/check/{type}")
  public ResponseEntity<?> check(@PathVariable String type) {
    var users = resolveUsers(type);
    var result = users.stream().map(u -> Map.of(
        "id", u.getId(),
        "email", u.getEmail(),
        "username", u.getUsername(),
        "role", u.getRole().name(),
        "appRoles", u.getAppRoles().stream().map(AppRole::getName).toList()
    )).toList();
    log.info("CHECK {}: {} users found: {}", type, result.size(), result);
    return ResponseEntity.ok(Map.of("type", type, "users", result));
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

    log.info("SEND type={} subject='{}'", recipientType, subject);

    var allUsersCheck = userRepository.findAll();
    log.info("SEND all {} users: {}", allUsersCheck.size(),
        allUsersCheck.stream().map(u -> u.getId() + ":" + u.getEmail() + ":role=" + u.getRole().name() + ":appRoles=" + u.getAppRoles().stream().map(AppRole::getName).toList()).toList());

    List<String> emails = resolveRecipients(recipientType);
    log.info("SEND resolveRecipients({}) returned {} emails: {}", recipientType, emails.size(), emails);

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

    createNotifications(recipientType, subject, messageBody);

    messageLogRepository.save(new MessageLog(recipientType, subject, messageBody, sent, Instant.now()));
    log.info("Notified {} / {} recipients (type={})", sent, emails.size(), recipientType);

    return ResponseEntity.ok(Map.of(
        "sent", sent,
        "total", emails.size(),
        "recipientType", recipientType));
  }

  private void createNotifications(String type, String title, String message) {
    List<User> users = resolveUsers(type);
    List<Notification> notifications = new ArrayList<>();
    for (User user : users) {
      notifications.add(new Notification(user, title, message, "MESSAGE", null));
    }
    if (!notifications.isEmpty()) {
      notificationRepository.saveAll(notifications);
      log.info("Created {} in-app notifications for type={}", notifications.size(), type);
    }
  }

  private List<User> resolveUsers(String type) {
    var all = userRepository.findAll();
    return switch (type) {
      case "ALL_RACERS" -> resolveRacers(all);
      case "ADMINS" -> resolveAdminUsers(all);
      case "JUDGES" -> resolveJudgeUsers(all);
      default -> List.of();
    };
  }

  private List<User> resolveRacers(List<User> all) {
    Edition edition = editionRepository.findTopByOrderByEditionYearDesc().orElse(null);
    if (edition == null) return List.of();
    var emails = racerRegistrationRepository.findByEditionOrderByStartNumber(edition).stream()
        .filter(r -> "PAID".equals(r.getStatus()))
        .map(RacerRegistration::getEmail)
        .filter(e -> e != null && !e.isBlank())
        .distinct()
        .toList();
    return all.stream()
        .filter(u -> u.getEmail() != null && emails.contains(u.getEmail()))
        .toList();
  }

  private List<User> resolveAdminUsers(List<User> all) {
    var adminRole = appRoleRepository.findByName("ADMIN");
    return all.stream()
        .filter(u -> u.getRole() == UserRole.ADMIN
            || (adminRole.isPresent()
                && u.getAppRoles().stream().anyMatch(r -> r.getId().equals(adminRole.get().getId()))))
        .toList();
  }

  private List<User> resolveJudgeUsers(List<User> all) {
    var judgeRole = appRoleRepository.findByName("JUDGE");
    var matched = all.stream()
        .filter(u -> {
          boolean byRole = u.getRole() == UserRole.JUDGE;
          boolean byAppRole = judgeRole.isPresent()
              && u.getAppRoles().stream().anyMatch(r -> r.getId().equals(judgeRole.get().getId()));
          if (!byRole && !byAppRole) {
            log.debug("resolveUsers JUDGES: skip user id={} email={} role={} appRoles={}",
                u.getId(), u.getEmail(), u.getRole(),
                u.getAppRoles().stream().map(AppRole::getName).toList());
          }
          return byRole || byAppRole;
        })
        .toList();
    log.info("resolveUsers JUDGES: {} matched out of {} total users. Matched: {}",
        matched.size(), all.size(),
        matched.stream().map(u -> u.getId() + ":" + u.getEmail() + ":role=" + u.getRole()).toList());
    return matched;
  }

  private List<String> resolveRecipients(String type) {
    return switch (type) {
      case "ALL_RACERS" -> {
        Edition edition = editionRepository.findTopByOrderByEditionYearDesc().orElse(null);
        if (edition == null) yield List.of();
        yield racerRegistrationRepository.findByEditionOrderByStartNumber(edition).stream()
            .filter(r -> "PAID".equals(r.getStatus()))
            .map(RacerRegistration::getEmail)
            .filter(e -> e != null && !e.isBlank())
            .distinct()
            .toList();
      }
      case "ADMINS" -> {
        var adminRole = appRoleRepository.findByName("ADMIN");
        yield userRepository.findAll().stream()
            .filter(u -> u.getRole() == UserRole.ADMIN
                || (adminRole.isPresent()
                    && u.getAppRoles().stream().anyMatch(r -> r.getId().equals(adminRole.get().getId()))))
            .map(User::getEmail)
            .filter(e -> e != null && !e.isBlank())
            .toList();
      }
      case "JUDGES" -> {
        var judgeRole = appRoleRepository.findByName("JUDGE");
        var all = userRepository.findAll();
        var matched = all.stream()
            .filter(u -> u.getRole() == UserRole.JUDGE
                || (judgeRole.isPresent()
                    && u.getAppRoles().stream().anyMatch(r -> r.getId().equals(judgeRole.get().getId()))))
            .toList();
        log.info("resolveRecipients JUDGES: {} matched out of {} total users. Matched: {}",
            matched.size(), all.size(),
            matched.stream().map(u -> u.getId() + ":" + u.getEmail() + ":role=" + u.getRole()).toList());
        yield matched.stream()
            .map(User::getEmail)
            .filter(e -> e != null && !e.isBlank())
            .toList();
      }
      default -> List.of();
    };
  }
}
