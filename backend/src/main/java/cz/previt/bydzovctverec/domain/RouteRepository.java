package cz.previt.bydzovctverec.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RouteRepository extends JpaRepository<Route, Long> {
  List<Route> findByEditionOrderByName(Edition edition);
  Optional<Route> findByEditionAndVariant(Edition edition, String variant);
  List<Route> findByEditionAndPublishedTrue(Edition edition);
}
