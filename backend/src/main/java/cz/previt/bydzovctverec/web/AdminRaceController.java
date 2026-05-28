package cz.previt.bydzovctverec.web;

import cz.previt.bydzovctverec.domain.Edition;
import cz.previt.bydzovctverec.domain.EditionRepository;
import cz.previt.bydzovctverec.service.EditionService;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/race")
public class AdminRaceController {

  private final EditionService editionService;
  private final EditionRepository editionRepository;

  public AdminRaceController(EditionService editionService, EditionRepository editionRepository) {
    this.editionService = editionService;
    this.editionRepository = editionRepository;
  }

  @GetMapping
  public ResponseEntity<?> status() {
    Edition edition = editionService.getCurrentEdition();
    if (edition == null) {
      return ResponseEntity.ok(Map.of("started", false, "finished", false));
    }
    return ResponseEntity.ok(Map.of(
        "started", edition.getRaceStartedAt() != null,
        "finished", edition.getRaceFinishedAt() != null));
  }

  @PostMapping("/start")
  @Transactional
  public ResponseEntity<?> start() {
    Edition edition = editionService.getCurrentEdition();
    if (edition == null) {
      return ResponseEntity.badRequest().body(ApiResponse.error("Žádný aktivní ročník"));
    }
    if (edition.getRaceFinishedAt() != null) {
      return ResponseEntity.badRequest().body(ApiResponse.error("Závod již byl ukončen"));
    }
    if (edition.getRaceStartedAt() != null) {
      return ResponseEntity.badRequest().body(ApiResponse.error("Závod již byl zahájen"));
    }
    edition.setRaceStartedAt(Instant.now());
    editionRepository.save(edition);
    Map<String, Object> resp = new LinkedHashMap<>();
    resp.put("started", true);
    resp.put("startedAt", edition.getRaceStartedAt());
    return ResponseEntity.ok(resp);
  }

  @PostMapping("/finish")
  @Transactional
  public ResponseEntity<?> finish() {
    Edition edition = editionService.getCurrentEdition();
    if (edition == null) {
      return ResponseEntity.badRequest().body(ApiResponse.error("Žádný aktivní ročník"));
    }
    if (edition.getRaceStartedAt() == null) {
      return ResponseEntity.badRequest().body(ApiResponse.error("Závod ještě nezačal"));
    }
    edition.setRaceFinishedAt(Instant.now());
    editionRepository.save(edition);
    Map<String, Object> resp = new LinkedHashMap<>();
    resp.put("finished", true);
    resp.put("finishedAt", edition.getRaceFinishedAt());
    return ResponseEntity.ok(resp);
  }

  @PostMapping("/reset")
  @Transactional
  public ResponseEntity<?> reset() {
    Edition edition = editionService.getCurrentEdition();
    if (edition == null) {
      return ResponseEntity.badRequest().body(ApiResponse.error("Žádný aktivní ročník"));
    }
    edition.setRaceStartedAt(null);
    edition.setRaceFinishedAt(null);
    editionRepository.save(edition);
    return ResponseEntity.ok(Map.of("started", false, "finished", false));
}
}
