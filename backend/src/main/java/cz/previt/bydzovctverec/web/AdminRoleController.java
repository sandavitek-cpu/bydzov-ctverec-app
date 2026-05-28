package cz.previt.bydzovctverec.web;

import cz.previt.bydzovctverec.domain.AppRole;
import cz.previt.bydzovctverec.domain.AppRoleRepository;
import cz.previt.bydzovctverec.domain.User;
import cz.previt.bydzovctverec.domain.UserRepository;
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
@RequestMapping("/api/admin/roles")
public class AdminRoleController {

  private final AppRoleRepository appRoleRepository;
  private final UserRepository userRepository;

  public AdminRoleController(AppRoleRepository appRoleRepository, UserRepository userRepository) {
    this.appRoleRepository = appRoleRepository;
    this.userRepository = userRepository;
  }

  @GetMapping
  public ResponseEntity<?> list() {
    var roles = appRoleRepository.findAll();
    var result = roles.stream().map(r -> {
      Map<String, Object> m = new LinkedHashMap<>();
      m.put("id", r.getId());
      m.put("name", r.getName());
      m.put("displayName", r.getDisplayName());
      m.put("createdAt", r.getCreatedAt().toString());
      return m;
    }).toList();
    return ResponseEntity.ok(result);
  }

  @GetMapping("/{id}/users")
  @Transactional(readOnly = true)
  public ResponseEntity<?> users(@PathVariable Long id) {
    var role = appRoleRepository.findById(id);
    if (role.isEmpty()) return ResponseEntity.notFound().build();
    var users = userRepository.findAll().stream()
        .filter(u -> u.getAppRoles().stream().anyMatch(r -> r.getId().equals(id)))
        .map(u -> Map.of("id", u.getId(), "email", u.getEmail(), "name", u.getName()))
        .toList();
    return ResponseEntity.ok(users);
  }

  @PostMapping
  public ResponseEntity<?> create(@RequestBody Map<String, String> body) {
    var name = body.get("name");
    var displayName = body.get("displayName");
    if (name == null || name.isBlank()) {
      return ResponseEntity.badRequest().body(ApiResponse.error("Chybí název role"));
    }
    if (appRoleRepository.findByName(name.toUpperCase()).isPresent()) {
      return ResponseEntity.badRequest().body(ApiResponse.error("Role již existuje"));
    }
    var role = appRoleRepository.save(new AppRole(name.toUpperCase(), displayName, Instant.now()));
    return ResponseEntity.ok(Map.of(
        "id", role.getId(),
        "name", role.getName(),
        "displayName", role.getDisplayName()));
  }

  @PutMapping("/{id}")
  public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Map<String, String> body) {
    var existing = appRoleRepository.findById(id);
    if (existing.isEmpty()) return ResponseEntity.notFound().build();
    var role = existing.get();
    var displayName = body.get("displayName");
    if (displayName != null && !displayName.isBlank()) {
      role.setDisplayName(displayName);
    }
    appRoleRepository.save(role);
    return ResponseEntity.ok(Map.of(
        "id", role.getId(),
        "name", role.getName(),
        "displayName", role.getDisplayName()));
  }

  @DeleteMapping("/{id}")
  @Transactional
  public ResponseEntity<?> delete(@PathVariable Long id) {
    if (!appRoleRepository.existsById(id)) {
      return ResponseEntity.badRequest().body(ApiResponse.error("Role nenalezena"));
    }
    boolean hasUsers = userRepository.findAll().stream()
        .anyMatch(u -> u.getAppRoles().stream().anyMatch(r -> r.getId().equals(id)));
    if (hasUsers) {
      return ResponseEntity.badRequest().body(ApiResponse.error("Roli nelze smazat – je přiřazena uživatelům"));
    }
    appRoleRepository.deleteById(id);
    return ResponseEntity.noContent().build();
  }
}
