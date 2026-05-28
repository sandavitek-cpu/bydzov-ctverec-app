package cz.previt.bydzovctverec.web;

import cz.previt.bydzovctverec.domain.Edition;
import cz.previt.bydzovctverec.domain.ScheduleItem;
import cz.previt.bydzovctverec.domain.ScheduleItemRepository;
import cz.previt.bydzovctverec.service.EditionService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
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
@RequestMapping("/api/admin/schedule")
public class AdminScheduleController {

  private final ScheduleItemRepository scheduleItemRepository;
  private final EditionService editionService;
  private final SimpMessagingTemplate messagingTemplate;

  public AdminScheduleController(ScheduleItemRepository scheduleItemRepository,
      EditionService editionService, SimpMessagingTemplate messagingTemplate) {
    this.scheduleItemRepository = scheduleItemRepository;
    this.editionService = editionService;
    this.messagingTemplate = messagingTemplate;
  }

  @GetMapping
  public ResponseEntity<?> list() {
    Edition edition = editionService.getCurrentEdition();
    if (edition == null) {
      return ResponseEntity.ok(ApiResponse.ok(List.of()));
    }
    return ResponseEntity.ok(
        scheduleItemRepository.findByEditionOrderBySortOrder(edition).stream()
            .map(this::toMap).toList());
  }

  @PostMapping
  @Transactional
  public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {
    Edition edition = editionService.getCurrentEdition();
    if (edition == null) {
      return ResponseEntity.badRequest().body(ApiResponse.error("Žádný aktivní ročník"));
    }
    String time = WebUtils.toString(body.get("time"));
    String label = WebUtils.toString(body.get("label"));
    if (time == null || label == null || time.isBlank() || label.isBlank()) {
      return ResponseEntity.badRequest().body(ApiResponse.error("time a label jsou povinné"));
    }
    ScheduleItem item = new ScheduleItem(edition, time, label,
        WebUtils.toString(body.get("description")),
        body.get("sortOrder") instanceof Number n ? n.intValue() : 0);
    scheduleItemRepository.save(item);
    broadcast();
    return ResponseEntity.ok(toMap(item));
  }

  @PutMapping("/{id}")
  @Transactional
  public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
    ScheduleItem item = scheduleItemRepository.findById(id).orElse(null);
    if (item == null) {
      return ResponseEntity.badRequest().body(ApiResponse.error("Položka nenalezena"));
    }
    if (body.containsKey("time")) item.setTime(WebUtils.toString(body.get("time")));
    if (body.containsKey("label")) item.setLabel(WebUtils.toString(body.get("label")));
    if (body.containsKey("description")) item.setDescription(WebUtils.toString(body.get("description")));
    if (body.containsKey("sortOrder") && body.get("sortOrder") instanceof Number n) item.setSortOrder(n.intValue());
    scheduleItemRepository.save(item);
    broadcast();
    return ResponseEntity.ok(toMap(item));
  }

  @DeleteMapping("/{id}")
  @Transactional
  public ResponseEntity<?> delete(@PathVariable Long id) {
    ScheduleItem item = scheduleItemRepository.findById(id).orElse(null);
    if (item == null) {
      return ResponseEntity.badRequest().body(ApiResponse.error("Položka nenalezena"));
    }
    scheduleItemRepository.delete(item);
    broadcast();
    return ResponseEntity.ok(ApiResponse.ok(Map.of("deleted", true)));
  }

  private void broadcast() {
    Edition edition = editionService.getCurrentEdition();
    if (edition == null) return;
    List<Map<String, Object>> items = scheduleItemRepository
        .findByEditionOrderBySortOrder(edition).stream()
        .map(this::toMap).toList();
    messagingTemplate.convertAndSend("/topic/schedule", Map.of("items", items));
  }

  private Map<String, Object> toMap(ScheduleItem item) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("id", item.getId());
    m.put("time", item.getTime());
    m.put("label", item.getLabel());
    m.put("description", item.getDescription());
    m.put("sortOrder", item.getSortOrder());
    return m;
  }
}
