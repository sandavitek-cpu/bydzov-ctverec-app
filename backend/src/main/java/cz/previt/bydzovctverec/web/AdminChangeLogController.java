package cz.previt.bydzovctverec.web;

import cz.previt.bydzovctverec.domain.ChangeLogEntry;
import cz.previt.bydzovctverec.domain.ChangeLogEntryRepository;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
@RequestMapping("/api/admin/changelog")
public class AdminChangeLogController {

  private final ChangeLogEntryRepository repository;

  public AdminChangeLogController(ChangeLogEntryRepository repository) {
    this.repository = repository;
  }

  @GetMapping
  public ResponseEntity<?> list() {
    return ResponseEntity.ok(repository.findAllByOrderByCreatedAtDesc().stream()
        .map(this::toMap).toList());
  }

  @PostMapping
  @Transactional
  public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {
    String version = WebUtils.toString(body.get("version"));
    if (version == null || version.isBlank()) {
      return ResponseEntity.badRequest().body(ApiResponse.error("Verze je povinná"));
    }
    String description = WebUtils.toString(body.get("description"));
    if (description == null || description.isBlank()) {
      return ResponseEntity.badRequest().body(ApiResponse.error("Popis je povinný"));
    }
    var entry = new ChangeLogEntry(version, description, Instant.now());
    repository.save(entry);
    return ResponseEntity.ok(toMap(entry));
  }

  @PutMapping("/{id}")
  @Transactional
  public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
    var entry = repository.findById(id).orElse(null);
    if (entry == null) {
      return ResponseEntity.badRequest().body(ApiResponse.error("Záznam nenalezen"));
    }
    if (body.containsKey("version")) entry.setVersion(WebUtils.toString(body.get("version")));
    if (body.containsKey("description")) entry.setDescription(WebUtils.toString(body.get("description")));
    repository.save(entry);
    return ResponseEntity.ok(toMap(entry));
  }

  @DeleteMapping("/{id}")
  @Transactional
  public ResponseEntity<?> delete(@PathVariable Long id) {
    var entry = repository.findById(id).orElse(null);
    if (entry == null) {
      return ResponseEntity.badRequest().body(ApiResponse.error("Záznam nenalezen"));
    }
    repository.delete(entry);
    return ResponseEntity.ok(Map.of("deleted", true));
  }

  private Map<String, Object> toMap(ChangeLogEntry e) {
    var m = new LinkedHashMap<String, Object>();
    m.put("id", e.getId());
    m.put("version", e.getVersion());
    m.put("description", e.getDescription());
    m.put("createdAt", e.getCreatedAt().toString());
    return m;
  }
}
