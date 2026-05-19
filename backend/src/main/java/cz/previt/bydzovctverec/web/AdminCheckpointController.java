package cz.previt.bydzovctverec.web;

import cz.previt.bydzovctverec.domain.Checkpoint;
import cz.previt.bydzovctverec.domain.CheckpointRepository;
import cz.previt.bydzovctverec.domain.Edition;
import cz.previt.bydzovctverec.domain.EditionRepository;
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
@RequestMapping("/api/admin/checkpoints")
public class AdminCheckpointController {

  private final EditionRepository editionRepository;
  private final CheckpointRepository checkpointRepository;

  public AdminCheckpointController(EditionRepository editionRepository, CheckpointRepository checkpointRepository) {
    this.editionRepository = editionRepository;
    this.checkpointRepository = checkpointRepository;
  }

  private Edition currentEdition() {
    return editionRepository.findTopByOrderByEditionYearDesc().orElse(null);
  }

  @GetMapping
  @Transactional(readOnly = true)
  public ResponseEntity<?> list() {
    Edition edition = currentEdition();
    if (edition == null) {
      return ResponseEntity.ok(List.of());
    }
    List<Checkpoint> items = checkpointRepository.findByEditionOrderBySortOrder(edition);
    return ResponseEntity.ok(items);
  }

  @PostMapping
  @Transactional
  public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {
    Edition edition = currentEdition();
    if (edition == null) {
      return ResponseEntity.badRequest().body(Map.of("error", "Žádný aktivní ročník"));
    }
    String name = (String) body.get("name");
    if (name == null || name.isBlank()) {
      return ResponseEntity.badRequest().body(Map.of("error", "Chybí název stanoviště"));
    }
    Double lat = body.get("lat") instanceof Number n ? n.doubleValue() : null;
    Double lng = body.get("lng") instanceof Number n ? n.doubleValue() : null;
    if (lat == null || lng == null) {
      return ResponseEntity.badRequest().body(Map.of("error", "Chybí souřadnice (lat/lng)"));
    }
    Checkpoint cp = new Checkpoint(edition, name, lat, lng,
        body.get("radius") instanceof Number n ? n.intValue() : 300,
        body.get("sortOrder") instanceof Number n ? n.intValue() : 0);
    cp.setTaskDescription((String) body.get("taskDescription"));
    cp.setMaxPoints(body.get("maxPoints") instanceof Number n ? n.intValue() : null);
    cp.setVolunteers(toStringList(body.get("volunteers")));
    checkpointRepository.save(cp);
    return ResponseEntity.ok(cp);
  }

  @PutMapping("/{id}")
  @Transactional
  public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
    Checkpoint cp = checkpointRepository.findById(id).orElse(null);
    if (cp == null) {
      return ResponseEntity.badRequest().body(Map.of("error", "Stanoviště nenalezeno"));
    }
    if (body.containsKey("name")) cp.setName((String) body.get("name"));
    if (body.containsKey("lat") && body.get("lat") instanceof Number n) cp.setLat(n.doubleValue());
    if (body.containsKey("lng") && body.get("lng") instanceof Number n) cp.setLng(n.doubleValue());
    if (body.containsKey("radius") && body.get("radius") instanceof Number n) cp.setRadius(n.intValue());
    if (body.containsKey("sortOrder") && body.get("sortOrder") instanceof Number n) cp.setSortOrder(n.intValue());
    if (body.containsKey("taskDescription")) cp.setTaskDescription((String) body.get("taskDescription"));
    if (body.containsKey("maxPoints") && body.get("maxPoints") instanceof Number n) cp.setMaxPoints(n.intValue());
    if (body.containsKey("volunteers")) cp.setVolunteers(toStringList(body.get("volunteers")));
    checkpointRepository.save(cp);
    return ResponseEntity.ok(cp);
  }

  @SuppressWarnings("unchecked")
  private List<String> toStringList(Object v) {
    if (v instanceof List<?> list) {
      return list.stream().map(Object::toString).toList();
    }
    return List.of();
  }

  @DeleteMapping("/{id}")
  @Transactional
  public ResponseEntity<?> delete(@PathVariable Long id) {
    if (!checkpointRepository.existsById(id)) {
      return ResponseEntity.badRequest().body(Map.of("error", "Stanoviště nenalezeno"));
    }
    checkpointRepository.deleteById(id);
    return ResponseEntity.ok(Map.of("deleted", true));
  }
}
