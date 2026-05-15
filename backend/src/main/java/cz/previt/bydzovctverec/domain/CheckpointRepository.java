package cz.previt.bydzovctverec.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CheckpointRepository extends JpaRepository<Checkpoint, Long> {

  List<Checkpoint> findByEditionOrderBySortOrder(Edition edition);
}
