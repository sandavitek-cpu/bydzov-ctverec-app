package cz.previt.bydzovctverec.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScoreRepository extends JpaRepository<Score, Long> {

  List<Score> findByRacerRegistrationIdOrderByRunNumber(Long racerRegistrationId);
}
