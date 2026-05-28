package cz.previt.bydzovctverec.web;

import cz.previt.bydzovctverec.domain.Edition;
import cz.previt.bydzovctverec.service.EditionService;
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

  private final EditionService editionService;
  private final CeremonyService ceremonyService;

  public AdminCeremonyController(EditionService editionService, CeremonyService ceremonyService) {
    this.editionService = editionService;
    this.ceremonyService = ceremonyService;
  }

  @GetMapping("/data")
  public ResponseEntity<?> data() {
    Edition edition = editionService.getCurrentEdition();
    if (edition == null) {
      return ResponseEntity.ok(ApiResponse.ok(Map.of("year", 0, "overall", List.of(), "categories", List.of())));
    }
    return ResponseEntity.ok(ceremonyService.generateCeremonyData(edition.getEditionYear()));
  }

  @PostMapping("/generate")
  @Transactional
  public ResponseEntity<?> generate() {
    Edition edition = editionService.getCurrentEdition();
    if (edition == null) {
      return ResponseEntity.badRequest().body(ApiResponse.error("Žádný aktivní ročník"));
    }
    int computedCount = ceremonyService.computeCategoryWinners();
    var data = ceremonyService.generateCeremonyData(edition.getEditionYear());
    return ResponseEntity.ok(ApiResponse.ok(Map.of("computed", computedCount, "data", data)));
  }
}
