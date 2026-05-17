package cz.previt.bydzovctverec.web;

import cz.previt.bydzovctverec.domain.ChangeLogEntryRepository;
import cz.previt.bydzovctverec.domain.EditionRepository;
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

  public PublicInfoController(BuildProperties buildProperties, ChangeLogEntryRepository changelogRepository,
      EditionRepository editionRepository) {
    this.buildProperties = buildProperties;
    this.changelogRepository = changelogRepository;
    this.editionRepository = editionRepository;
  }

  @GetMapping("/info")
  public Map<String, Object> info() {
    var version = buildProperties.getVersion();
    var deployedAt = buildProperties.getTime().toString();
    var changelog = changelogRepository.findAllByOrderByCreatedAtDesc().stream()
        .map(e -> Map.<String, Object>of(
            "version", e.getVersion(),
            "description", e.getDescription(),
            "createdAt", e.getCreatedAt().toString()))
        .toList();

    var edition = editionRepository.findTopByOrderByEditionYearDesc().orElse(null);
    Map<String, Object> race = Map.of(
        "started", edition != null && edition.getRaceStartedAt() != null,
        "finished", edition != null && edition.getRaceFinishedAt() != null,
        "startedAt", edition != null && edition.getRaceStartedAt() != null ? edition.getRaceStartedAt().toString() : null,
        "finishedAt", edition != null && edition.getRaceFinishedAt() != null ? edition.getRaceFinishedAt().toString() : null
    );
    return Map.of("version", version, "deployedAt", deployedAt, "changelog", changelog, "race", race);
  }
}
