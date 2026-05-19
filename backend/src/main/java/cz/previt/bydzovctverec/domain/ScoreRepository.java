package cz.previt.bydzovctverec.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ScoreRepository extends JpaRepository<Score, Long> {

  @Query("SELECT s FROM Score s JOIN FETCH s.checkpoint cp WHERE s.racerRegistration.id = :racerRegistrationId ORDER BY cp.sortOrder")
  List<Score> findByRacerRegistrationIdWithCheckpoint(@Param("racerRegistrationId") Long racerRegistrationId);

  @Query("SELECT s FROM Score s JOIN FETCH s.racerRegistration r JOIN FETCH s.checkpoint cp WHERE r.edition.editionYear = :year ORDER BY r.id, cp.sortOrder")
  List<Score> findByEditionYearWithRacer(@Param("year") Integer year);

  List<Score> findByJudgeId(Long judgeId);

  List<Score> findByCheckpointId(Long checkpointId);

  @Query("SELECT s FROM Score s JOIN FETCH s.checkpoint cp WHERE s.judge.id = :judgeId AND s.checkpoint.id IN :checkpointIds")
  List<Score> findByJudgeIdAndCheckpointIdIn(@Param("judgeId") Long judgeId, @Param("checkpointIds") List<Long> checkpointIds);
}
