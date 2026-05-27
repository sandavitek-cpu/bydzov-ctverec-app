package cz.previt.bydzovctverec.service;

import cz.previt.bydzovctverec.domain.Edition;
import cz.previt.bydzovctverec.domain.EditionRepository;
import org.springframework.stereotype.Service;

@Service
public class EditionService {

  private final EditionRepository editionRepository;

  public EditionService(EditionRepository editionRepository) {
    this.editionRepository = editionRepository;
  }

  public Edition getCurrentEdition() {
    return editionRepository.findTopByOrderByEditionYearDesc().orElse(null);
  }

  public Edition getOrCreateCurrentEdition() {
    Edition edition = editionRepository.findTopByOrderByEditionYearDesc().orElse(null);
    if (edition == null) {
      edition = editionRepository.save(
          new Edition(2026, "30. ročník Novobydžovského čtverce – Memoriál Elišky Junkové"));
    }
    return edition;
  }

  public Edition getByYear(Integer year) {
    return editionRepository.findByEditionYear(year).orElse(null);
  }
}
