package cz.previt.bydzovctverec.web;

import cz.previt.bydzovctverec.domain.ChangeLogEntryRepository;
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

  public PublicInfoController(BuildProperties buildProperties, ChangeLogEntryRepository changelogRepository) {
    this.buildProperties = buildProperties;
    this.changelogRepository = changelogRepository;
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
    return Map.of("version", version, "deployedAt", deployedAt, "changelog", changelog);
  }
}
