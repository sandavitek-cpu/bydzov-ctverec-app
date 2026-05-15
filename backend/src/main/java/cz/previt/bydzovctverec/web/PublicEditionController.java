package cz.previt.bydzovctverec.web;

import cz.previt.bydzovctverec.domain.Checkpoint;
import cz.previt.bydzovctverec.domain.CheckpointRepository;
import cz.previt.bydzovctverec.domain.Edition;
import cz.previt.bydzovctverec.domain.EditionRepository;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/editions")
public class PublicEditionController {

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
      edition = editionRepository.save(new Edition(2026, "30. ročník Novobydžovského čtverce"));
    }
    List<Checkpoint> cps = checkpointRepository.findByEditionOrderBySortOrder(edition);
    List<AreaResponse> areas = cps.stream()
        .map(c -> new AreaResponse(c.getName(), c.getLat(), c.getLng(), c.getRadius()))
        .toList();
    return ResponseEntity.ok(new EditionResponse(edition.getId(), edition.getEditionYear(), edition.getLabel(), areas));
  }

  public record EditionResponse(Long id, int year, String label, List<AreaResponse> areas) {}
  public record AreaResponse(String name, Double lat, Double lng, Integer radius) {}
}
