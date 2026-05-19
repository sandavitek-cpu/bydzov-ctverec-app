package cz.previt.bydzovctverec.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

  List<Notification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(Long userId);

  List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

  long countByUserIdAndIsReadFalse(Long userId);
}
