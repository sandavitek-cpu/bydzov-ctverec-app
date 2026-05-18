package cz.previt.bydzovctverec.web;

import cz.previt.bydzovctverec.domain.Edition;
import cz.previt.bydzovctverec.domain.EditionRepository;
import cz.previt.bydzovctverec.domain.RaceCategory;
import cz.previt.bydzovctverec.domain.RaceCategoryRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/ceremony")
public class PublicCeremonyController {

  private final RaceCategoryRepository raceCategoryRepository;
  private final EditionRepository editionRepository;

  public PublicCeremonyController(RaceCategoryRepository raceCategoryRepository,
      EditionRepository editionRepository) {
    this.raceCategoryRepository = raceCategoryRepository;
    this.editionRepository = editionRepository;
  }

  @GetMapping("/{year}")
  public ResponseEntity<?> ceremony(@PathVariable Integer year) {
    Edition edition = editionRepository.findByEditionYear(year).orElse(null);
    if (edition == null) {
      return ResponseEntity.badRequest().body(Map.of("error", "Ročník nenalezen"));
    }

    List<Map<String, Object>> cats = raceCategoryRepository
        .findByEditionOrderBySortOrder(edition)
        .stream()
        .map(this::toMap)
        .toList();

    return ResponseEntity.ok(Map.of("year", year, "categories", cats));
  }

  private Map<String, Object> toMap(RaceCategory cat) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("id", cat.getId());
    m.put("name", cat.getName());
    m.put("sortOrder", cat.getSortOrder());
    m.put("winnerName", cat.getWinnerName());
    m.put("winnerTeam", cat.getWinnerTeam());
    m.put("winnerNumber", cat.getWinnerNumber());
    m.put("winnerPoints", cat.getWinnerPoints());
    return m;
  }
}
