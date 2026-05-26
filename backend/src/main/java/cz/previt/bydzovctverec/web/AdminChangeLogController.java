package cz.previt.bydzovctverec.web;

import cz.previt.bydzovctverec.domain.ChangeLogEntryRepository;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
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

}
