package cz.previt.bydzovctverec.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VariantConfigRepository extends JpaRepository<VariantConfig, Long> {
  List<VariantConfig> findByEdition(Edition edition);
}
