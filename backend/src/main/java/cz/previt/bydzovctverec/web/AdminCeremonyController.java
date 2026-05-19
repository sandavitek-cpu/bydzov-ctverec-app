package cz.previt.bydzovctverec.web;

import cz.previt.bydzovctverec.domain.Edition;
import cz.previt.bydzovctverec.domain.EditionRepository;
import cz.previt.bydzovctverec.service.CeremonyService;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/ceremony")
public class AdminCeremonyController {

  private final EditionRepository editionRepository;
  private final CeremonyService ceremonyService;

  public AdminCeremonyController(EditionRepository editionRepository, CeremonyService ceremonyService) {
    this.editionRepository = editionRepository;
    this.ceremonyService = ceremonyService;
  }

  @GetMapping("/data")
  public ResponseEntity<?> data() {
    Edition edition = editionRepository.findTopByOrderByEditionYearDesc().orElse(null);
    if (edition == null) {
      return ResponseEntity.ok(Map.of("year", 0, "overall", List.of(), "categories", List.of()));
    }
    return ResponseEntity.ok(ceremonyService.generateCeremonyData(edition.getEditionYear()));
  }

  @PostMapping("/generate")
  @Transactional
  public ResponseEntity<?> generate() {
    Edition edition = editionRepository.findTopByOrderByEditionYearDesc().orElse(null);
    if (edition == null) {
      return ResponseEntity.badRequest().body(Map.of("error", "Žádný aktivní ročník"));
    }
    int computedCount = ceremonyService.computeCategoryWinners();
    var data = ceremonyService.generateCeremonyData(edition.getEditionYear());
    return ResponseEntity.ok(Map.of("computed", computedCount, "data", data));
  }
}
