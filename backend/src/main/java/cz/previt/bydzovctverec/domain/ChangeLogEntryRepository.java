package cz.previt.bydzovctverec.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChangeLogEntryRepository extends JpaRepository<ChangeLogEntry, Long> {
  List<ChangeLogEntry> findAllByOrderByCreatedAtDesc();
  boolean existsByVersion(String version);
}
