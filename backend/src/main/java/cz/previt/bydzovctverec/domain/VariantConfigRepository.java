package cz.previt.bydzovctverec.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VariantConfigRepository extends JpaRepository<VariantConfig, Long> {
  List<VariantConfig> findByEdition(Edition edition);
  Optional<VariantConfig> findByEditionAndVariantCode(Edition edition, String variantCode);
}
