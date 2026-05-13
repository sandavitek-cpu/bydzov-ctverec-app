package cz.previt.bydzovctverec.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EditionRepository extends JpaRepository<Edition, Long> {

  Optional<Edition> findTopByOrderByEditionYearDesc();
}
