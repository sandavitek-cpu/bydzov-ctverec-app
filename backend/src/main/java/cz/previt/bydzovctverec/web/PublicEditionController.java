package cz.previt.bydzovctverec.web;

import cz.previt.bydzovctverec.domain.Checkpoint;
import cz.previt.bydzovctverec.domain.CheckpointRepository;
import cz.previt.bydzovctverec.domain.Edition;
import cz.previt.bydzovctverec.domain.EditionRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/editions")
public class PublicEditionController {

  private static final Logger log = LoggerFactory.getLogger(PublicEditionController.class);
  private final EditionRepository editionRepository;
  private final CheckpointRepository checkpointRepository;

  public PublicEditionController(EditionRepository editionRepository, CheckpointRepository checkpointRepository) {
    this.editionRepository = editionRepository;
    this.checkpointRepository = checkpointRepository;
  }

  @GetMapping("/current")
  public ResponseEntity<EditionResponse> current() {
    Edition edition = editionRepository.findTopByOrderByEditionYearDesc().orElse(null);
    if (edition == null) {
      edition = editionRepository.save(new Edition(2026, "30. ročník Novobydžovského čtverce – Memoriál Elišky Junkové"));
      seedCheckpoints(edition);
    }

    List<Checkpoint> existing = checkpointRepository.findByEditionOrderBySortOrder(edition);
    if (existing.isEmpty()) {
      seedCheckpoints(edition);
      existing = checkpointRepository.findByEditionOrderBySortOrder(edition);
    }

    List<AreaResponse> areas = existing.stream()
        .map(c -> new AreaResponse(c.getName(), c.getLat(), c.getLng(), c.getRadius()))
        .toList();
    return ResponseEntity.ok(new EditionResponse(edition.getId(), edition.getEditionYear(), edition.getLabel(), areas));
  }

  private void seedCheckpoints(Edition edition) {
    log.info("Seeding checkpoints for edition {}", edition.getEditionYear());
    checkpointRepository.saveAll(List.of(
        new Checkpoint(edition, "Prezence / Start", 50.2415, 15.4900, 300, 1),
        new Checkpoint(edition, "Stanoviště 1 — Chlumec", 50.2280, 15.4750, 500, 2),
        new Checkpoint(edition, "Stanoviště 2 — Hlušice", 50.2150, 15.4600, 500, 3),
        new Checkpoint(edition, "Stanoviště 3 — Sloupno", 50.2520, 15.5050, 500, 4),
        new Checkpoint(edition, "Dojezd / Cíl", 50.2415, 15.4900, 300, 5)));
  }

  public record EditionResponse(Long id, int year, String label, List<AreaResponse> areas) {}
  public record AreaResponse(String name, Double lat, Double lng, Integer radius) {}
}
