package cz.previt.bydzovctverec.web;

import cz.previt.bydzovctverec.domain.EditionRepository;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/debug")
public class DebugController {

  private final EditionRepository editionRepository;

  public DebugController(EditionRepository editionRepository) {
    this.editionRepository = editionRepository;
  }

  @GetMapping("/edition-check")
  public ResponseEntity<?> check() {
    var all = editionRepository.findAll();
    var top = editionRepository.findTopByOrderByEditionYearDesc();
    var byYear = editionRepository.findByEditionYear(2026);
    return ResponseEntity.ok(Map.of(
        "allCount", all.size(),
        "topPresent", top.isPresent(),
        "byYearPresent", byYear.isPresent()));
  }
}
