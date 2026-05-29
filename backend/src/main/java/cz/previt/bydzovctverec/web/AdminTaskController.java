package cz.previt.bydzovctverec.web;

import cz.previt.bydzovctverec.domain.Task;
import cz.previt.bydzovctverec.domain.TaskRepository;
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
@RequestMapping("/api/admin/tasks")
public class AdminTaskController {

  private final TaskRepository taskRepository;

  public AdminTaskController(TaskRepository taskRepository) {
    this.taskRepository = taskRepository;
  }

  @GetMapping
  public ResponseEntity<?> list() {
    return ResponseEntity.ok(taskRepository.findAllByOrderByName().stream()
        .map(this::toMap).toList());
  }

  @PostMapping
  @Transactional
  public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {
    String name = WebUtils.toString(body.get("name"));
    if (name == null || name.isBlank()) {
      return ResponseEntity.badRequest().body(ApiResponse.error("Název je povinný"));
    }
    Task task = new Task(name);
    if (body.containsKey("description")) task.setDescription(WebUtils.toString(body.get("description")));
    if (body.containsKey("recommendedPoints") && body.get("recommendedPoints") instanceof Number n) {
      task.setRecommendedPoints(n.intValue());
    }
    if (body.containsKey("tools")) task.setTools(WebUtils.toString(body.get("tools")));
    taskRepository.save(task);
    return ResponseEntity.ok(toMap(task));
  }

  @PutMapping("/{id}")
  @Transactional
  public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
    Task task = taskRepository.findById(id).orElse(null);
    if (task == null) return ResponseEntity.badRequest().body(ApiResponse.error("Úkol nenalezen"));
    if (body.containsKey("name")) task.setName(WebUtils.toString(body.get("name")));
    if (body.containsKey("description")) task.setDescription(WebUtils.toString(body.get("description")));
    if (body.containsKey("recommendedPoints") && body.get("recommendedPoints") instanceof Number n) {
      task.setRecommendedPoints(n.intValue());
    }
    if (body.containsKey("tools")) task.setTools(WebUtils.toString(body.get("tools")));
    taskRepository.save(task);
    return ResponseEntity.ok(toMap(task));
  }

  @DeleteMapping("/{id}")
  @Transactional
  public ResponseEntity<?> delete(@PathVariable Long id) {
    Task task = taskRepository.findById(id).orElse(null);
    if (task == null) return ResponseEntity.badRequest().body(ApiResponse.error("Úkol nenalezen"));
    taskRepository.delete(task);
    return ResponseEntity.ok(Map.of("deleted", true));
  }

  private Map<String, Object> toMap(Task task) {
    var m = new LinkedHashMap<String, Object>();
    m.put("id", task.getId());
    m.put("name", task.getName());
    m.put("description", task.getDescription());
    m.put("recommendedPoints", task.getRecommendedPoints());
    m.put("tools", task.getTools());
    m.put("createdAt", task.getCreatedAt());
    return m;
  }
}
