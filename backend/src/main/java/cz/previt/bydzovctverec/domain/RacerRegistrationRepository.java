package cz.previt.bydzovctverec.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RacerRegistrationRepository extends JpaRepository<RacerRegistration, Long> {

  Optional<RacerRegistration> findByEmail(String email);

  Optional<RacerRegistration> findTopByEditionOrderByStartNumberDesc(Edition edition);

  Optional<RacerRegistration> findTopByEditionOrderByPaymentReferenceDesc(Edition edition);

  List<RacerRegistration> findByEditionOrderByStartNumber(Edition edition);

  List<RacerRegistration> findByEditionId(Long editionId);

  Optional<RacerRegistration> findByEditionAndStartNumber(Edition edition, Integer startNumber);

  @Modifying
  @Query("UPDATE RacerRegistration r SET r.status = :status WHERE r.id = :id")
  void updateStatus(@Param("id") Long id, @Param("status") String status);
}
