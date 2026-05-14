package cz.previt.bydzovctverec.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScheduleItemRepository extends JpaRepository<ScheduleItem, Long> {

  List<ScheduleItem> findByEditionOrderBySortOrder(Edition edition);
}
