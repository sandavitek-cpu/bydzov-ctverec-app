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
    Checkpoint cp = new Checkpoint(edition,
        (String) body.get("name"),
        body.get("lat") instanceof Number n ? n.doubleValue() : null,
        body.get("lng") instanceof Number n ? n.doubleValue() : null,
        body.get("radius") instanceof Number n ? n.intValue() : 300,
        body.get("sortOrder") instanceof Number n ? n.intValue() : 0);
    cp.setTaskDescription((String) body.get("taskDescription"));
    cp.setMaxPoints(body.get("maxPoints") instanceof Number n ? n.intValue() : null);
    cp.setVolunteerName((String) body.get("volunteerName"));
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
    if (body.containsKey("lat")) cp.setLat(((Number) body.get("lat")).doubleValue());
    if (body.containsKey("lng")) cp.setLng(((Number) body.get("lng")).doubleValue());
    if (body.containsKey("radius")) cp.setRadius(((Number) body.get("radius")).intValue());
    if (body.containsKey("sortOrder")) cp.setSortOrder(((Number) body.get("sortOrder")).intValue());
    if (body.containsKey("taskDescription")) cp.setTaskDescription((String) body.get("taskDescription"));
    if (body.containsKey("maxPoints")) cp.setMaxPoints(((Number) body.get("maxPoints")).intValue());
    if (body.containsKey("volunteerName")) cp.setVolunteerName((String) body.get("volunteerName"));
    checkpointRepository.save(cp);
    return ResponseEntity.ok(cp);
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
