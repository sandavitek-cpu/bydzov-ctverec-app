package cz.previt.bydzovctverec.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ScoreRepository extends JpaRepository<Score, Long> {

  List<Score> findByRacerRegistrationIdOrderByRunNumber(Long racerRegistrationId);

  @Query("SELECT s FROM Score s JOIN FETCH s.racerRegistration r WHERE r.edition.editionYear = :year ORDER BY r.id, s.runNumber")
  List<Score> findByEditionYearWithRacer(@Param("year") Integer year);
}
