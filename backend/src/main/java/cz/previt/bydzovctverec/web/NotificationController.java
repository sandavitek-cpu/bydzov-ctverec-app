package cz.previt.bydzovctverec.web;

import cz.previt.bydzovctverec.domain.Notification;
import cz.previt.bydzovctverec.domain.NotificationRepository;
import cz.previt.bydzovctverec.domain.User;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

  private final NotificationRepository notificationRepository;

  public NotificationController(NotificationRepository notificationRepository) {
    this.notificationRepository = notificationRepository;
  }

  @GetMapping
  public ResponseEntity<?> list(@AuthenticationPrincipal User user) {
    List<Notification> notifications = notificationRepository
        .findByUserIdOrderByCreatedAtDesc(user.getId());
    long unreadCount = notificationRepository.countByUserIdAndIsReadFalse(user.getId());
    return ResponseEntity.ok(Map.of(
        "notifications", notifications.stream().map(this::toDto).toList(),
        "unreadCount", unreadCount));
  }

  @PatchMapping("/{id}/read")
  @Transactional
  public ResponseEntity<?> markRead(@PathVariable Long id, @AuthenticationPrincipal User user) {
    Notification notification = notificationRepository.findById(id).orElse(null);
    if (notification == null || !notification.getUser().getId().equals(user.getId())) {
      return ResponseEntity.notFound().build();
    }
    notification.markRead();
    notificationRepository.save(notification);
    return ResponseEntity.ok().build();
  }

  @PostMapping("/read-all")
  @Transactional
  public ResponseEntity<?> markAllRead(@AuthenticationPrincipal User user) {
    List<Notification> unread = notificationRepository
        .findByUserIdAndIsReadFalseOrderByCreatedAtDesc(user.getId());
    unread.forEach(Notification::markRead);
    notificationRepository.saveAll(unread);
    return ResponseEntity.ok().build();
  }

  private Map<String, Object> toDto(Notification n) {
    return Map.of(
        "id", n.getId(),
        "title", n.getTitle(),
        "message", n.getMessage(),
        "type", n.getType(),
        "relatedUrl", n.getRelatedUrl(),
        "isRead", n.getIsRead(),
        "createdAt", n.getCreatedAt().toString());
  }
}
