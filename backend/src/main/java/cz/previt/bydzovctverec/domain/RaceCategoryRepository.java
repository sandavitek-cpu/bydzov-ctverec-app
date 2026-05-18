package cz.previt.bydzovctverec.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RaceCategoryRepository extends JpaRepository<RaceCategory, Long> {
  List<RaceCategory> findByEditionOrderBySortOrder(Edition edition);
  List<RaceCategory> findByEditionIdOrderBySortOrder(Long editionId);
}
