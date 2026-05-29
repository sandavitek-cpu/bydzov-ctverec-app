package cz.previt.bydzovctverec.web;

import cz.previt.bydzovctverec.domain.User;
import cz.previt.bydzovctverec.service.IncidentService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/incidents")
public class IncidentController {

  private final IncidentService incidentService;

  public IncidentController(IncidentService incidentService) {
    this.incidentService = incidentService;
  }

  @GetMapping
  public ResponseEntity<?> listMy(@AuthenticationPrincipal User user) {
    var assignments = incidentService.getUserAssignments(user.getId());
    var dtos = assignments.stream().map(a -> {
      var inc = a.getIncident();
      Map<String, Object> m = new LinkedHashMap<>();
      m.put("assigneeId", a.getId());
      m.put("assigneeStatus", a.getStatus());
      m.put("id", inc.getId());
      m.put("title", inc.getTitle());
      m.put("description", inc.getDescription());
      m.put("dueDate", inc.getDueDate() != null ? inc.getDueDate().toString() : null);
      m.put("incidentStatus", inc.getStatus());
      m.put("createdById", inc.getCreatedBy().getId());
      m.put("createdByName", inc.getCreatedBy().getName());
      m.put("createdAt", inc.getCreatedAt().toString());
      m.put("updatedAt", inc.getUpdatedAt().toString());
      return m;
    }).toList();
    return ResponseEntity.ok(dtos);
  }

  @PutMapping("/{assigneeId}/status")
  @Transactional
  public ResponseEntity<?> updateStatus(@PathVariable Long assigneeId,
      @RequestBody Map<String, String> body,
      @AuthenticationPrincipal User user) {
    var newStatus = body.get("status");
    if (newStatus == null || (!newStatus.equals("IN_PROGRESS") && !newStatus.equals("DONE"))) {
      return ResponseEntity.badRequest().body(ApiResponse.error("Neplatný stav (IN_PROGRESS / DONE)"));
    }
    incidentService.updateAssigneeStatus(assigneeId, user, newStatus);
    return ResponseEntity.ok(Map.of("message", "Stav změněn"));
  }
}
