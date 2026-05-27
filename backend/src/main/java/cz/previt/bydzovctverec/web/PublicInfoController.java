package cz.previt.bydzovctverec.web;

import cz.previt.bydzovctverec.config.FeeConfig;
import cz.previt.bydzovctverec.domain.ChangeLogEntryRepository;
import cz.previt.bydzovctverec.domain.Edition;
import cz.previt.bydzovctverec.domain.EditionRepository;
import cz.previt.bydzovctverec.domain.VariantConfigRepository;
import cz.previt.bydzovctverec.service.EditionService;
import java.util.Map;
import org.springframework.boot.info.BuildProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public")
public class PublicInfoController {

  private final BuildProperties buildProperties;
  private final ChangeLogEntryRepository changelogRepository;
  private final EditionService editionService;
  private final VariantConfigRepository variantConfigRepository;
  private final FeeConfig feeConfig;

  public PublicInfoController(BuildProperties buildProperties,
      ChangeLogEntryRepository changelogRepository, EditionService editionService,
      VariantConfigRepository variantConfigRepository, FeeConfig feeConfig) {
    this.buildProperties = buildProperties;
    this.changelogRepository = changelogRepository;
    this.editionService = editionService;
    this.variantConfigRepository = variantConfigRepository;
    this.feeConfig = feeConfig;
  }

  @GetMapping("/info")
  public ResponseEntity<?> info() {
    Edition edition = editionService.getCurrentEdition();
    boolean raceStarted = edition != null && edition.getRaceStartedAt() != null;
    boolean raceFinished = edition != null && edition.getRaceFinishedAt() != null;
    return ResponseEntity.ok(Map.of(
        "version", buildProperties.getVersion(),
        "changelog", changelogRepository.findAllByOrderByCreatedAtDesc(),
        "raceStarted", raceStarted,
        "raceFinished", raceFinished));
  }

  @GetMapping("/fees")
  public ResponseEntity<?> fees() {
    return ResponseEntity.ok(feeConfig.feeSchedule());
  }

  @GetMapping("/variants")
  public ResponseEntity<?> variants() {
    Edition edition = editionService.getCurrentEdition();
    if (edition == null) return ResponseEntity.ok(java.util.List.of());
    return ResponseEntity.ok(variantConfigRepository.findByEdition(edition));
  }
}
