package cz.previt.bydzovctverec.web;

import cz.previt.bydzovctverec.config.FeeConfig;
import cz.previt.bydzovctverec.config.FeeConfig.VariantFee;
import cz.previt.bydzovctverec.domain.ChangeLogEntryRepository;
import cz.previt.bydzovctverec.domain.Edition;
import cz.previt.bydzovctverec.domain.EditionRepository;
import cz.previt.bydzovctverec.domain.VariantConfig;
import cz.previt.bydzovctverec.domain.VariantConfigRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.info.BuildProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public")
public class PublicInfoController {

  private final BuildProperties buildProperties;
  private final ChangeLogEntryRepository changelogRepository;
  private final EditionRepository editionRepository;
  private final VariantConfigRepository variantConfigRepository;
  private final FeeConfig feeConfig;

  public PublicInfoController(BuildProperties buildProperties, ChangeLogEntryRepository changelogRepository,
      EditionRepository editionRepository, VariantConfigRepository variantConfigRepository,
      FeeConfig feeConfig) {
    this.buildProperties = buildProperties;
    this.changelogRepository = changelogRepository;
    this.editionRepository = editionRepository;
    this.variantConfigRepository = variantConfigRepository;
    this.feeConfig = feeConfig;
  }

  @GetMapping("/info")
  public Map<String, Object> info() {
    var version = buildProperties.getVersion();
    var deployedAt = buildProperties.getTime() != null ? buildProperties.getTime().toString() : null;
    var changelog = changelogRepository.findAllByOrderByCreatedAtDesc().stream()
        .map(e -> Map.<String, Object>of(
            "version", e.getVersion(),
            "description", e.getDescription(),
            "createdAt", e.getCreatedAt().toString()))
        .toList();

    var edition = editionRepository.findTopByOrderByEditionYearDesc().orElse(null);
    Map<String, Object> race = new LinkedHashMap<>();
    race.put("started", edition != null && edition.getRaceStartedAt() != null);
    race.put("finished", edition != null && edition.getRaceFinishedAt() != null);
    race.put("startedAt", edition != null && edition.getRaceStartedAt() != null ? edition.getRaceStartedAt().toString() : null);
    race.put("finishedAt", edition != null && edition.getRaceFinishedAt() != null ? edition.getRaceFinishedAt().toString() : null);
    return Map.of("version", version, "deployedAt", deployedAt, "changelog", changelog, "race", race);
  }

  @GetMapping("/fees")
  public Map<String, VariantFee> fees() {
    return feeConfig.feeSchedule();
  }

  @GetMapping("/variants")
  public List<Map<String, Object>> variants() {
    Edition edition = editionRepository.findTopByOrderByEditionYearDesc().orElse(null);
    if (edition == null) return List.of();
    return variantConfigRepository.findByEdition(edition).stream()
        .filter(VariantConfig::isEnabled)
        .map(vc -> {
          Map<String, Object> m = new LinkedHashMap<>();
          m.put("variantCode", vc.getVariantCode());
          m.put("label", vc.getLabel());
          m.put("registrationDeadline", vc.getRegistrationDeadline() != null ? vc.getRegistrationDeadline().toString() : null);
          m.put("registrationReopenedUntil", vc.getRegistrationReopenedUntil() != null ? vc.getRegistrationReopenedUntil().toString() : null);
          m.put("raceDate", vc.getRaceDate() != null ? vc.getRaceDate().toString() : null);
          return m;
        }).toList();
  }
}
