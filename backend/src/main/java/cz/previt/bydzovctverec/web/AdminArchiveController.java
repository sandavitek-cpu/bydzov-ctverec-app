package cz.previt.bydzovctverec.web;

import cz.previt.bydzovctverec.domain.ArchiveEntry;
import cz.previt.bydzovctverec.domain.ArchiveEntryRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/archive")
public class AdminArchiveController {

  private final ArchiveEntryRepository archiveEntryRepository;

  public AdminArchiveController(ArchiveEntryRepository archiveEntryRepository) {
    this.archiveEntryRepository = archiveEntryRepository;
  }

  @GetMapping
  public ResponseEntity<?> list() {
    List<ArchiveEntry> entries = archiveEntryRepository.findAllByOrderByEditionYearDescRankAsc();
    return ResponseEntity.ok(entries);
  }

  @PostMapping
  @Transactional
  public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {
    Integer editionYear = (Integer) body.get("editionYear");
    Integer rank = (Integer) body.get("rank");
    String racerName = (String) body.get("racerName");
    String vehicle = (String) body.get("vehicle");
    Integer points = (Integer) body.get("points");

    if (editionYear == null || rank == null || racerName == null || points == null) {
      return ResponseEntity.badRequest().body(Map.of("error", "Chybí povinná pole"));
    }

    ArchiveEntry entry = new ArchiveEntry(editionYear, rank, racerName, vehicle, points);
    archiveEntryRepository.save(entry);
    return ResponseEntity.ok(entry);
  }

  @DeleteMapping("/{id}")
  @Transactional
  public ResponseEntity<?> delete(@PathVariable Long id) {
    if (!archiveEntryRepository.existsById(id)) {
      return ResponseEntity.badRequest().body(Map.of("error", "Záznam nenalezen"));
    }
    archiveEntryRepository.deleteById(id);
    return ResponseEntity.ok(Map.of("deleted", true));
  }

  @PostMapping(value = "/import", consumes = MediaType.TEXT_PLAIN_VALUE)
  @Transactional
  public ResponseEntity<?> importCsv(@RequestBody String csvBody) {
    List<ArchiveEntry> saved = new ArrayList<>();
    String[] lines = csvBody.split("\n");
    boolean first = true;
    for (String line : lines) {
      if (first) { first = false; continue; }
      line = line.trim();
      if (line.isBlank()) continue;
      String[] parts = line.split(",", -1);
      if (parts.length < 4) continue;
      try {
        Integer year = Integer.parseInt(parts[0].trim());
        Integer rank = Integer.parseInt(parts[1].trim());
        String name = parts[2].trim();
        String veh = parts.length > 3 ? parts[3].trim() : null;
        Integer pts = parts.length > 4 && !parts[4].isBlank() ? Integer.parseInt(parts[4].trim()) : 0;
        saved.add(archiveEntryRepository.save(new ArchiveEntry(year, rank, name, veh, pts)));
      } catch (NumberFormatException ignored) {}
    }
    return ResponseEntity.ok(Map.of("imported", saved.size()));
  }
}
