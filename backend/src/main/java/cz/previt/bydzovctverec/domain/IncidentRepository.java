package cz.previt.bydzovctverec.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IncidentRepository extends JpaRepository<Incident, Long> {

  List<Incident> findByCreatedByIdOrderByCreatedAtDesc(Long createdById);

  List<Incident> findAllByOrderByCreatedAtDesc();
}
