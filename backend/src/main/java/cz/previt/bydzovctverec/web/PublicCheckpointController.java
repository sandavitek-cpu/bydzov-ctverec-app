package cz.previt.bydzovctverec.web;

import cz.previt.bydzovctverec.domain.Checkpoint;
import cz.previt.bydzovctverec.domain.CheckpointRepository;
import cz.previt.bydzovctverec.domain.Edition;
import cz.previt.bydzovctverec.service.EditionService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/areas")
public class PublicCheckpointController {

  private final EditionService editionService;
  private final CheckpointRepository checkpointRepository;

  public PublicCheckpointController(EditionService editionService, CheckpointRepository checkpointRepository) {
    this.editionService = editionService;
    this.checkpointRepository = checkpointRepository;
  }

  @GetMapping("/{year}")
  public ResponseEntity<?> list(@PathVariable Integer year) {
    Edition edition = editionService.getByYear(year);
    if (edition == null) {
      return ResponseEntity.badRequest().body(ApiResponse.error("Ročník nenalezen"));
    }
    List<Checkpoint> items = checkpointRepository.findByEditionOrderBySortOrder(edition);
    return ResponseEntity.ok(items.stream().map(c -> new CheckpointResponse(c.getName(), c.getLat(), c.getLng(), c.getRadius())).toList());
  }

  public record CheckpointResponse(String name, Double lat, Double lng, Integer radius) {}
}
