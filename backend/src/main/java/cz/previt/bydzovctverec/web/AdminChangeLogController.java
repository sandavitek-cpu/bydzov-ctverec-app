package cz.previt.bydzovctverec.web;

import cz.previt.bydzovctverec.domain.ChangeLogEntry;
import cz.previt.bydzovctverec.domain.ChangeLogEntryRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/changelog")
public class AdminChangeLogController {

  private final ChangeLogEntryRepository repository;

  public AdminChangeLogController(ChangeLogEntryRepository repository) {
    this.repository = repository;
  }

  @GetMapping
  public List<Map<String, Object>> list() {
    return repository.findAllByOrderByCreatedAtDesc().stream()
        .map(e -> Map.<String, Object>of(
            "id", e.getId(),
            "version", e.getVersion(),
            "description", e.getDescription(),
            "createdAt", e.getCreatedAt().toString()))
        .toList();
  }

  @PostMapping
  @Transactional
  public ResponseEntity<?> create(@RequestBody Map<String, String> body) {
    var version = body.get("version");
    var description = body.get("description");
    if (version == null || version.isBlank() || description == null || description.isBlank()) {
      return ResponseEntity.badRequest().body(Map.of("error", "Chybí verze nebo popis"));
    }
    var entry = new ChangeLogEntry(version, description, Instant.now());
    repository.save(entry);
    return ResponseEntity.ok(Map.of("id", entry.getId(), "message", "Záznam přidán"));
  }
}
