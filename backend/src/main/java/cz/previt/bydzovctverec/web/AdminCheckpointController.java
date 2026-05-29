package cz.previt.bydzovctverec.web;

import cz.previt.bydzovctverec.domain.Checkpoint;
import cz.previt.bydzovctverec.domain.CheckpointRepository;
import cz.previt.bydzovctverec.domain.RacerRegistration;
import cz.previt.bydzovctverec.domain.RacerRegistrationRepository;
import cz.previt.bydzovctverec.domain.ScoreRepository;
import cz.previt.bydzovctverec.domain.Edition;
import cz.previt.bydzovctverec.domain.Task;
import cz.previt.bydzovctverec.domain.TaskRepository;
import cz.previt.bydzovctverec.service.EditionService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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

  private final EditionService editionService;
  private final CheckpointRepository checkpointRepository;
  private final RacerRegistrationRepository racerRegistrationRepository;
  private final ScoreRepository scoreRepository;
  private final TaskRepository taskRepository;

  public AdminCheckpointController(EditionService editionService,
      CheckpointRepository checkpointRepository,
      RacerRegistrationRepository racerRegistrationRepository,
      ScoreRepository scoreRepository,
      TaskRepository taskRepository) {
    this.editionService = editionService;
    this.checkpointRepository = checkpointRepository;
    this.racerRegistrationRepository = racerRegistrationRepository;
    this.scoreRepository = scoreRepository;
    this.taskRepository = taskRepository;
  }

  @GetMapping
  public ResponseEntity<?> list() {
    Edition edition = editionService.getCurrentEdition();
    if (edition == null) return ResponseEntity.ok(List.of());
    return ResponseEntity.ok(checkpointRepository.findByEditionOrderBySortOrder(edition).stream()
        .map(this::toMap).toList());
  }

  @GetMapping("/progress")
  public ResponseEntity<?> progress() {
    Edition edition = editionService.getCurrentEdition();
    if (edition == null) return ResponseEntity.ok(Map.of());
    List<Checkpoint> checkpoints = checkpointRepository.findByEditionOrderBySortOrder(edition);
    List<RacerRegistration> activeRacers = racerRegistrationRepository.findByEditionOrderByStartNumber(edition)
        .stream().filter(r -> "PAID".equals(r.getStatus())).toList();
    var allScores = scoreRepository.findByEditionYearWithRacer(edition.getEditionYear());
    var scoredByCp = allScores.stream()
        .collect(Collectors.groupingBy(s -> s.getCheckpoint().getId()));
    List<Map<String, Object>> cpProgress = new ArrayList<>();
    int totalScored = 0;
    int totalComplete = 0;
    for (Checkpoint cp : checkpoints) {
      List<?> cpScores = scoredByCp.getOrDefault(cp.getId(), List.of());
      int count = cpScores.size();
      boolean complete = count >= activeRacers.size();
      totalScored += count;
      if (complete) totalComplete++;
      cpProgress.add(Map.of("id", cp.getId(), "name", cp.getName(), "scoredCount", count, "complete", complete));
    }
    var result = new LinkedHashMap<String, Object>();
    result.put("checkpoints", cpProgress);
    result.put("totalCheckpoints", checkpoints.size());
    result.put("totalScored", totalScored);
    result.put("totalComplete", totalComplete);
    result.put("totalRacers", activeRacers.size());
    return ResponseEntity.ok(result);
  }

  @PostMapping
  @Transactional
  public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {
    Edition edition = editionService.getCurrentEdition();
    if (edition == null) return ResponseEntity.badRequest().body(ApiResponse.error("Žádný aktivní ročník"));
    Checkpoint cp = new Checkpoint(edition, WebUtils.toString(body.get("name")),
        body.get("lat") != null ? ((Number) body.get("lat")).doubleValue() : null,
        body.get("lng") != null ? ((Number) body.get("lng")).doubleValue() : null,
        body.get("radius") != null ? ((Number) body.get("radius")).intValue() : 300,
        body.get("sortOrder") != null ? ((Number) body.get("sortOrder")).intValue() : 0);
    if (body.containsKey("taskDescription")) cp.setTaskDescription(WebUtils.toString(body.get("taskDescription")));
    if (body.containsKey("maxPoints")) cp.setMaxPoints(((Number) body.get("maxPoints")).intValue());
    if (body.containsKey("volunteers")) cp.setVolunteers(toStringList(body.get("volunteers")));
    if (body.containsKey("taskIds")) cp.setTasks(resolveTasks(body.get("taskIds")));
    checkpointRepository.save(cp);
    return ResponseEntity.ok(toMap(cp));
  }

  @PutMapping("/{id}")
  @Transactional
  public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
    Checkpoint cp = checkpointRepository.findById(id).orElse(null);
    if (cp == null) return ResponseEntity.badRequest().body(ApiResponse.error("Kontrola nenalezena"));
    if (body.containsKey("name")) cp.setName(WebUtils.toString(body.get("name")));
    if (body.containsKey("lat") && body.get("lat") instanceof Number n) cp.setLat(n.doubleValue());
    if (body.containsKey("lng") && body.get("lng") instanceof Number n) cp.setLng(n.doubleValue());
    if (body.containsKey("radius") && body.get("radius") instanceof Number n) cp.setRadius(n.intValue());
    if (body.containsKey("sortOrder") && body.get("sortOrder") instanceof Number n) cp.setSortOrder(n.intValue());
    if (body.containsKey("taskDescription")) cp.setTaskDescription(WebUtils.toString(body.get("taskDescription")));
    if (body.containsKey("maxPoints") && body.get("maxPoints") instanceof Number n) cp.setMaxPoints(n.intValue());
    if (body.containsKey("volunteers")) cp.setVolunteers(toStringList(body.get("volunteers")));
    if (body.containsKey("taskIds")) cp.setTasks(resolveTasks(body.get("taskIds")));
    checkpointRepository.save(cp);
    return ResponseEntity.ok(toMap(cp));
  }

  @DeleteMapping("/{id}")
  @Transactional
  public ResponseEntity<?> delete(@PathVariable Long id) {
    Checkpoint cp = checkpointRepository.findById(id).orElse(null);
    if (cp == null) return ResponseEntity.badRequest().body(ApiResponse.error("Kontrola nenalezena"));
    scoreRepository.findByCheckpointId(id).forEach(scoreRepository::delete);
    checkpointRepository.delete(cp);
    return ResponseEntity.ok(Map.of("deleted", true));
  }

  private Map<String, Object> toMap(Checkpoint cp) {
    var m = new LinkedHashMap<String, Object>();
    m.put("id", cp.getId());
    m.put("name", cp.getName());
    m.put("lat", cp.getLat());
    m.put("lng", cp.getLng());
    m.put("radius", cp.getRadius());
    m.put("sortOrder", cp.getSortOrder());
    m.put("taskDescription", cp.getTaskDescription());
    m.put("maxPoints", cp.getMaxPoints());
    m.put("volunteers", cp.getVolunteers());
    m.put("taskIds", cp.getTasks().stream().map(Task::getId).toList());
    return m;
  }

  @SuppressWarnings("unchecked")
  private List<String> toStringList(Object v) {
    if (v instanceof List<?> list) {
      return ((List<Object>) list).stream().map(Object::toString).toList();
    }
    return List.of();
  }

  @SuppressWarnings("unchecked")
  private List<Task> resolveTasks(Object v) {
    if (v instanceof List<?> list) {
      List<Long> ids = ((List<Object>) list).stream()
          .map(x -> x instanceof Number n ? n.longValue() : Long.valueOf(x.toString()))
          .toList();
      return taskRepository.findAllById(ids);
    }
    return List.of();
  }
}
