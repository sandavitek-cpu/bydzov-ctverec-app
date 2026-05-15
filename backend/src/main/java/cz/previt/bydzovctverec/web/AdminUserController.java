package cz.previt.bydzovctverec.web;

import cz.previt.bydzovctverec.domain.AppRole;
import cz.previt.bydzovctverec.domain.AppRoleRepository;
import cz.previt.bydzovctverec.domain.User;
import cz.previt.bydzovctverec.domain.UserRepository;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

  private final UserRepository userRepository;
  private final AppRoleRepository appRoleRepository;

  public AdminUserController(UserRepository userRepository, AppRoleRepository appRoleRepository) {
    this.userRepository = userRepository;
    this.appRoleRepository = appRoleRepository;
  }

  @GetMapping
  public ResponseEntity<?> list(@RequestParam(required = false) String q) {
    var users = userRepository.findAll();
    if (q != null && !q.isBlank()) {
      var query = q.toLowerCase();
      users = users.stream()
          .filter(u -> u.getName().toLowerCase().contains(query)
              || u.getEmail().toLowerCase().contains(query))
          .toList();
    }
    var result = users.stream().map(u -> Map.of(
        "id", u.getId(),
        "email", u.getEmail(),
        "username", u.getUsername(),
        "name", u.getName(),
        "role", u.getRole().name(),
        "createdAt", u.getCreatedAt().toString(),
        "appRoles", u.getAppRoles().stream().map(r -> Map.of(
            "id", r.getId(),
            "name", r.getName(),
            "displayName", r.getDisplayName())).toList())).toList();
    return ResponseEntity.ok(result);
  }

  @GetMapping("/{id}")
  @Transactional(readOnly = true)
  public ResponseEntity<?> get(@PathVariable Long id) {
    var user = userRepository.findById(id);
    if (user.isEmpty()) return ResponseEntity.notFound().build();
    var u = user.get();
    return ResponseEntity.ok(Map.of(
        "id", u.getId(),
        "email", u.getEmail(),
        "username", u.getUsername(),
        "name", u.getName(),
        "role", u.getRole().name(),
        "createdAt", u.getCreatedAt().toString(),
        "appRoles", u.getAppRoles().stream().map(r -> Map.of(
            "id", r.getId(),
            "name", r.getName(),
            "displayName", r.getDisplayName())).toList()));
  }

  @PutMapping("/{id}")
  @Transactional
  public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Map<String, String> body) {
    var userOpt = userRepository.findById(id);
    if (userOpt.isEmpty()) return ResponseEntity.notFound().build();
    var user = userOpt.get();
    var name = body.get("name");
    var email = body.get("email");
    if (name != null && !name.isBlank()) user.setName(name);
    if (email != null && !email.isBlank()) {
      if (!email.equals(user.getEmail()) && userRepository.findByEmail(email).isPresent()) {
        return ResponseEntity.badRequest().body(Map.of("error", "Email již existuje"));
      }
      user.setEmail(email);
    }
    userRepository.save(user);
    return ResponseEntity.ok(Map.of(
        "id", user.getId(),
        "email", user.getEmail(),
        "username", user.getUsername(),
        "name", user.getName()));
  }

  @PostMapping("/{userId}/roles")
  @Transactional
  public ResponseEntity<?> addRole(@PathVariable Long userId, @RequestBody Map<String, Object> body) {
    var user = userRepository.findById(userId);
    if (user.isEmpty()) return ResponseEntity.notFound().build();
    var roleId = body.get("roleId");
    if (roleId == null) return ResponseEntity.badRequest().body(Map.of("error", "Chybí roleId"));
    var role = appRoleRepository.findById(((Number) roleId).longValue());
    if (role.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "Role neexistuje"));
    user.get().getAppRoles().add(role.get());
    userRepository.save(user.get());
    return ResponseEntity.ok(Map.of("message", "Role přidána"));
  }

  @DeleteMapping("/{userId}/roles/{roleId}")
  @Transactional
  public ResponseEntity<?> removeRole(@PathVariable Long userId, @PathVariable Long roleId) {
    var userOpt = userRepository.findById(userId);
    if (userOpt.isEmpty()) return ResponseEntity.notFound().build();
    var user = userOpt.get();
    user.getAppRoles().removeIf(r -> r.getId().equals(roleId));
    userRepository.save(user);
    return ResponseEntity.noContent().build();
  }
}
