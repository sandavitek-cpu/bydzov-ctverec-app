package cz.previt.bydzovctverec.web;

import cz.previt.bydzovctverec.domain.Edition;
import cz.previt.bydzovctverec.domain.RaceCategory;
import cz.previt.bydzovctverec.domain.RaceCategoryRepository;
import cz.previt.bydzovctverec.domain.RacerRegistration;
import cz.previt.bydzovctverec.domain.RacerRegistrationRepository;
import cz.previt.bydzovctverec.domain.Score;
import cz.previt.bydzovctverec.domain.ScoreRepository;
import cz.previt.bydzovctverec.service.CeremonyService;
import cz.previt.bydzovctverec.service.EditionService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/categories")
public class AdminCategoryController {

  private final RaceCategoryRepository raceCategoryRepository;
  private final EditionService editionService;
  private final CeremonyService ceremonyService;

  public AdminCategoryController(RaceCategoryRepository raceCategoryRepository,
      EditionService editionService, CeremonyService ceremonyService) {
    this.raceCategoryRepository = raceCategoryRepository;
    this.editionService = editionService;
    this.ceremonyService = ceremonyService;
  }

  @GetMapping
  public ResponseEntity<?> list() {
    Edition edition = editionService.getCurrentEdition();
    if (edition == null) {
      return ResponseEntity.ok(ApiResponse.ok(List.of()));
    }
    return ResponseEntity.ok(
        raceCategoryRepository.findByEditionOrderBySortOrder(edition).stream()
            .map(this::toMap).toList());
  }

  @PostMapping
  @Transactional
  public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {
    Edition edition = editionService.getCurrentEdition();
    if (edition == null) {
      return ResponseEntity.badRequest().body(ApiResponse.error("Žádný aktivní ročník"));
    }
    String name = WebUtils.toString(body.get("name"));
    if (name == null || name.isBlank()) {
      return ResponseEntity.badRequest().body(ApiResponse.error("Název kategorie je povinný"));
    }
    RaceCategory cat = new RaceCategory(edition, name,
        WebUtils.toString(body.get("code")),
        WebUtils.toString(body.get("variant")),
        WebUtils.toString(body.get("determination")),
        body.get("sortOrder") instanceof Number n ? n.intValue() : 0);
    raceCategoryRepository.save(cat);
    return ResponseEntity.ok(toMap(cat));
  }

  @PutMapping("/{id}")
  @Transactional
  public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
    RaceCategory cat = raceCategoryRepository.findById(id).orElse(null);
    if (cat == null) {
      return ResponseEntity.badRequest().body(ApiResponse.error("Kategorie nenalezena"));
    }
    if (body.containsKey("name")) cat.setName(WebUtils.toString(body.get("name")));
    if (body.containsKey("code")) cat.setCode(WebUtils.toString(body.get("code")));
    if (body.containsKey("variant")) cat.setVariant(WebUtils.toString(body.get("variant")));
    if (body.containsKey("determination")) cat.setDetermination(WebUtils.toString(body.get("determination")));
    if (body.containsKey("sortOrder") && body.get("sortOrder") instanceof Number n) cat.setSortOrder(n.intValue());
    if (body.containsKey("winnerRegistrationId")) {
      cat.setWinnerRegistrationId(body.get("winnerRegistrationId") != null
          ? ((Number) body.get("winnerRegistrationId")).longValue() : null);
    }
    if (body.containsKey("winnerName")) cat.setWinnerName(WebUtils.toString(body.get("winnerName")));
    if (body.containsKey("winnerTeam")) cat.setWinnerTeam(WebUtils.toString(body.get("winnerTeam")));
    if (body.containsKey("winnerNumber")) {
      cat.setWinnerNumber(body.get("winnerNumber") != null
          ? ((Number) body.get("winnerNumber")).intValue() : null);
    }
    if (body.containsKey("winnerPoints")) {
      cat.setWinnerPoints(body.get("winnerPoints") != null
          ? ((Number) body.get("winnerPoints")).intValue() : null);
    }
    raceCategoryRepository.save(cat);
    return ResponseEntity.ok(toMap(cat));
  }

  @DeleteMapping("/{id}")
  @Transactional
  public ResponseEntity<?> delete(@PathVariable Long id) {
    RaceCategory cat = raceCategoryRepository.findById(id).orElse(null);
    if (cat == null) {
      return ResponseEntity.badRequest().body(ApiResponse.error("Kategorie nenalezena"));
    }
    raceCategoryRepository.delete(cat);
    return ResponseEntity.ok(ApiResponse.ok(Map.of("deleted", true)));
  }

  @PostMapping("/compute")
  @Transactional
  public ResponseEntity<?> computeWinners() {
    Edition edition = editionService.getCurrentEdition();
    if (edition == null) {
      return ResponseEntity.badRequest().body(ApiResponse.error("Žádný aktivní ročník"));
    }
    int count = ceremonyService.computeCategoryWinners();
    return ResponseEntity.ok(ApiResponse.ok(Map.of("computed", true, "count", count)));
  }

  private Map<String, Object> toMap(RaceCategory cat) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("id", cat.getId());
    m.put("name", cat.getName());
    m.put("code", cat.getCode());
    m.put("variant", cat.getVariant());
    m.put("determination", cat.getDetermination());
    m.put("sortOrder", cat.getSortOrder());
    m.put("winnerRegistrationId", cat.getWinnerRegistrationId());
    m.put("winnerName", cat.getWinnerName());
    m.put("winnerTeam", cat.getWinnerTeam());
    m.put("winnerNumber", cat.getWinnerNumber());
    m.put("winnerPoints", cat.getWinnerPoints());
    return m;
  }
}
