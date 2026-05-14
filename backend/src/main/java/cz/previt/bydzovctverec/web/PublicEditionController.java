package cz.previt.bydzovctverec.web;

import cz.previt.bydzovctverec.domain.Edition;
import cz.previt.bydzovctverec.domain.EditionRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/editions")
public class PublicEditionController {

  private final EditionRepository editionRepository;

  public PublicEditionController(EditionRepository editionRepository) {
    this.editionRepository = editionRepository;
  }

  @GetMapping("/current")
  public ResponseEntity<EditionResponse> current() {
    Edition edition = editionRepository.findTopByOrderByEditionYearDesc().orElse(null);
    if (edition == null) {
      edition = editionRepository.save(new Edition(2026, "30. ročník Novobydžovského čtverce"));
    }
    return ResponseEntity.ok(new EditionResponse(edition.getId(), edition.getEditionYear(), edition.getLabel()));
  }

  public record EditionResponse(Long id, int year, String label) {}
}
