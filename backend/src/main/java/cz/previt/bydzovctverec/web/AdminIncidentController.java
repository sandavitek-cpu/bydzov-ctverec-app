package cz.previt.bydzovctverec.web;

import cz.previt.bydzovctverec.domain.Incident;
import cz.previt.bydzovctverec.domain.IncidentRepository;
import cz.previt.bydzovctverec.domain.User;
import cz.previt.bydzovctverec.domain.UserRepository;
import cz.previt.bydzovctverec.service.IncidentService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/incidents")
public class AdminIncidentController {

  private final IncidentService incidentService;
  private final IncidentRepository incidentRepository;
  private final UserRepository userRepository;

  public AdminIncidentController(IncidentService incidentService,
      IncidentRepository incidentRepository, UserRepository userRepository) {
    this.incidentService = incidentService;
    this.incidentRepository = incidentRepository;
    this.userRepository = userRepository;
  }

  @GetMapping
  public ResponseEntity<?> list() {
    var incidents = incidentService.getAllIncidents();
    return ResponseEntity.ok(incidents.stream().map(this::toDto).toList());
  }

  @PostMapping
  @Transactional
  public ResponseEntity<?> create(@RequestBody Map<String, Object> body,
      @AuthenticationPrincipal User admin) {
    var title = getStr(body, "title");
    if (title == null || title.isBlank()) {
      return ResponseEntity.badRequest().body(ApiResponse.error("Název je povinný"));
    }
    var description = getStr(body, "description");
    var dueDateStr = getStr(body, "dueDate");
    var dueDate = dueDateStr != null && !dueDateStr.isBlank()
        ? LocalDate.parse(dueDateStr) : null;

    var assigneeIdsRaw = body.get("assigneeIds");
    List<User> assignees = new ArrayList<>();
    if (assigneeIdsRaw instanceof List<?> ids) {
      for (var idObj : ids) {
        if (idObj instanceof Number num) {
          userRepository.findById(num.longValue()).ifPresent(assignees::add);
        }
      }
    }
    if (assignees.isEmpty()) {
      return ResponseEntity.badRequest().body(ApiResponse.error("Vyberte alespoň jednoho řešitele"));
    }

    var incident = incidentService.createIncident(title, description, dueDate, admin, assignees);
    return ResponseEntity.ok(toDto(incident));
  }

  @DeleteMapping("/{id}")
  @Transactional
  public ResponseEntity<?> delete(@PathVariable Long id) {
    var opt = incidentRepository.findById(id);
    if (opt.isEmpty()) return ResponseEntity.notFound().build();
    incidentRepository.delete(opt.get());
    return ResponseEntity.noContent().build();
  }

  private Map<String, Object> toDto(Incident inc) {
    return Map.of(
        "id", inc.getId(),
        "title", inc.getTitle(),
        "description", inc.getDescription(),
        "dueDate", inc.getDueDate() != null ? inc.getDueDate().toString() : null,
        "status", inc.getStatus(),
        "createdById", inc.getCreatedBy().getId(),
        "createdByName", inc.getCreatedBy().getName(),
        "createdAt", inc.getCreatedAt().toString(),
        "updatedAt", inc.getUpdatedAt().toString(),
        "assignees", inc.getAssignees().stream().map(a -> Map.of(
            "id", a.getId(),
            "userId", a.getUser().getId(),
            "userName", a.getUser().getName(),
            "email", a.getUser().getEmail(),
            "status", a.getStatus(),
            "createdAt", a.getCreatedAt().toString())).toList());
  }

  private static String getStr(Map<String, Object> map, String key) {
    var v = map.get(key);
    return v instanceof String s ? s : null;
  }
}
