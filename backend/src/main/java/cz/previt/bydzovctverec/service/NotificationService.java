package cz.previt.bydzovctverec.service;

import cz.previt.bydzovctverec.domain.AppRoleRepository;
import cz.previt.bydzovctverec.domain.Edition;
import cz.previt.bydzovctverec.domain.EditionRepository;
import cz.previt.bydzovctverec.domain.Notification;
import cz.previt.bydzovctverec.domain.NotificationRepository;
import cz.previt.bydzovctverec.domain.RacerRegistration;
import cz.previt.bydzovctverec.domain.RacerRegistrationRepository;
import cz.previt.bydzovctverec.domain.User;
import cz.previt.bydzovctverec.domain.UserRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

  private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

  private final NotificationRepository notificationRepository;
  private final UserRepository userRepository;
  private final AppRoleRepository appRoleRepository;
  private final RacerRegistrationRepository racerRegistrationRepository;
  private final EditionRepository editionRepository;

  public NotificationService(NotificationRepository notificationRepository,
      UserRepository userRepository, AppRoleRepository appRoleRepository,
      RacerRegistrationRepository racerRegistrationRepository,
      EditionRepository editionRepository) {
    this.notificationRepository = notificationRepository;
    this.userRepository = userRepository;
    this.appRoleRepository = appRoleRepository;
    this.racerRegistrationRepository = racerRegistrationRepository;
    this.editionRepository = editionRepository;
  }

  @Transactional
  public void notifyAdmins(String title, String message, String link) {
    var adminRole = appRoleRepository.findByName("ADMIN");
    if (adminRole.isEmpty()) return;
    Long adminRoleId = adminRole.get().getId();
    List<User> admins = userRepository.findAll().stream()
        .filter(u -> u.getAppRoles().stream().anyMatch(r -> r.getId().equals(adminRoleId)))
        .toList();
    List<Notification> notifications = new ArrayList<>();
    for (User admin : admins) {
      notifications.add(new Notification(admin, title, message, "SUCCESS", link));
    }
    if (!notifications.isEmpty()) {
      notificationRepository.saveAll(notifications);
    }
  }

  public List<User> resolveAdmins() {
    return resolveByRole("ADMIN");
  }

  public List<User> resolveJudges() {
    return resolveByRole("JUDGE");
  }

  public List<User> resolveRacers() {
    Edition edition = editionRepository.findTopByOrderByEditionYearDesc().orElse(null);
    if (edition == null) return List.of();
    return racerRegistrationRepository.findByEditionOrderByStartNumber(edition).stream()
        .filter(r -> "PAID".equals(r.getStatus()))
        .map(r -> userRepository.findByEmail(r.getEmail()).orElse(null))
        .filter(u -> u != null)
        .toList();
  }

  private List<User> resolveByRole(String roleName) {
    var role = appRoleRepository.findByName(roleName);
    if (role.isEmpty()) return List.of();
    Long roleId = role.get().getId();
    return userRepository.findAll().stream()
        .filter(u -> u.getAppRoles().stream().anyMatch(r -> r.getId().equals(roleId)))
        .toList();
  }
}
