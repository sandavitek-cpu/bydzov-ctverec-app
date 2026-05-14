package cz.previt.bydzovctverec.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RacerRegistrationRepository extends JpaRepository<RacerRegistration, Long> {

  Optional<RacerRegistration> findByEmail(String email);
}
