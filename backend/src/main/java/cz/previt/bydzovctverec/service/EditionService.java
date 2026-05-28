package cz.previt.bydzovctverec.service;

import cz.previt.bydzovctverec.domain.Edition;
import cz.previt.bydzovctverec.domain.EditionRepository;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EditionService {

  private final EditionRepository editionRepository;

  public EditionService(EditionRepository editionRepository) {
    this.editionRepository = editionRepository;
  }

  public Edition getCurrentEdition() {
    return editionRepository.findTopByOrderByEditionYearDesc().orElse(null);
  }

  @Transactional
  public Edition getOrCreateCurrentEdition() {
    Edition edition = editionRepository.findTopByOrderByEditionYearDesc().orElse(null);
    if (edition == null) {
      int year = LocalDate.now().getYear();
      edition = editionRepository.save(
          new Edition(year, year + ". ročník Novobydžovského čtverce – Memoriál Elišky Junkové"));
    }
    return edition;
  }

  public Edition getByYear(Integer year) {
    return editionRepository.findByEditionYear(year).orElse(null);
  }
}
