package cz.previt.bydzovctverec.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IncidentAssigneeRepository extends JpaRepository<IncidentAssignee, Long> {

  List<IncidentAssignee> findByUserIdOrderByCreatedAtDesc(Long userId);

  List<IncidentAssignee> findByIncidentId(Long incidentId);
}
