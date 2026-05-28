package cz.previt.bydzovctverec.web;

import cz.previt.bydzovctverec.config.JwtService;
import cz.previt.bydzovctverec.domain.AppRole;
import cz.previt.bydzovctverec.domain.AppRoleRepository;
import cz.previt.bydzovctverec.domain.User;
import cz.previt.bydzovctverec.domain.UserRepository;
import cz.previt.bydzovctverec.domain.UserRole;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
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
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;

  public AdminUserController(UserRepository userRepository, AppRoleRepository appRoleRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
    this.userRepository = userRepository;
    this.appRoleRepository = appRoleRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
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
    var result = users.stream().map(u -> {
      var phone = u.getPhone() != null ? u.getPhone() : "";
      var memberSince = u.getMemberSince() != null ? u.getMemberSince().toString() : "";
      java.util.Map<String, Object> m = new java.util.HashMap<>();
      m.put("id", u.getId());
      m.put("email", u.getEmail());
      m.put("username", u.getUsername());
      m.put("firstName", u.getFirstName());
      m.put("lastName", u.getLastName());
      m.put("name", u.getName());
      m.put("phone", phone);
      m.put("memberSince", memberSince);
      m.put("role", u.getRole().name());
      m.put("createdAt", u.getCreatedAt().toString());
      m.put("appRoles", u.getAppRoles().stream().map(r -> java.util.Map.of(
          "id", r.getId(),
          "name", r.getName(),
          "displayName", r.getDisplayName())).toList());
      return m;
    }).toList();
    return ResponseEntity.ok(result);
  }

  @GetMapping("/{id}")
  @Transactional(readOnly = true)
  public ResponseEntity<?> get(@PathVariable Long id) {
    var user = userRepository.findById(id);
    if (user.isEmpty()) return ResponseEntity.notFound().build();
    var u = user.get();
    var phone = u.getPhone() != null ? u.getPhone() : "";
    var memberSince = u.getMemberSince() != null ? u.getMemberSince().toString() : "";
    java.util.Map<String, Object> m = new java.util.HashMap<>();
    m.put("id", u.getId());
    m.put("email", u.getEmail());
    m.put("username", u.getUsername());
    m.put("firstName", u.getFirstName());
    m.put("lastName", u.getLastName());
    m.put("name", u.getName());
    m.put("phone", phone);
    m.put("memberSince", memberSince);
    m.put("role", u.getRole().name());
    m.put("createdAt", u.getCreatedAt().toString());
    m.put("appRoles", u.getAppRoles().stream().map(r -> java.util.Map.of(
        "id", r.getId(),
        "name", r.getName(),
        "displayName", r.getDisplayName())).toList());
    return ResponseEntity.ok(m);
  }

  @PutMapping("/{id}")
  @Transactional
  public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
    var userOpt = userRepository.findById(id);
    if (userOpt.isEmpty()) return ResponseEntity.notFound().build();
    var user = userOpt.get();
    var firstName = getStr(body, "firstName");
    var lastName = getStr(body, "lastName");
    var email = getStr(body, "email");
    var phone = getStr(body, "phone");
    if (firstName != null && !firstName.isBlank()) user.setFirstName(firstName);
    if (lastName != null) user.setLastName(lastName);
    if (email != null && !email.isBlank()) {
      if (!email.equals(user.getEmail()) && userRepository.findByEmail(email).isPresent()) {
        return ResponseEntity.badRequest().body(ApiResponse.error("Email již existuje"));
      }
      user.setEmail(email);
    }
    if (phone != null) user.setPhone(phone);
    var memberSinceStr = getStr(body, "memberSince");
    if (memberSinceStr != null && !memberSinceStr.isBlank()) {
      user.setMemberSince(LocalDate.parse(memberSinceStr));
    }
    var roleStr = getStr(body, "role");
    if (roleStr != null) {
      try { user.setRole(UserRole.valueOf(roleStr.toUpperCase())); } catch (IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(ApiResponse.error("Neplatná role: " + roleStr));
      }
    }
    userRepository.save(user);
    var respPhone = user.getPhone() != null ? user.getPhone() : "";
    var respMemberSince = user.getMemberSince() != null ? user.getMemberSince().toString() : "";
    java.util.Map<String, Object> m = new java.util.HashMap<>();
    m.put("id", user.getId());
    m.put("email", user.getEmail());
    m.put("username", user.getUsername());
    m.put("firstName", user.getFirstName());
    m.put("lastName", user.getLastName());
    m.put("name", user.getName());
    m.put("phone", respPhone);
    m.put("memberSince", respMemberSince);
    m.put("role", user.getRole().name());
    m.put("createdAt", user.getCreatedAt().toString());
    return ResponseEntity.ok(m);
  }

  @PostMapping
  @Transactional
  public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {
    var username = getStr(body, "username");
    var email = getStr(body, "email");
    var password = getStr(body, "password");
    var firstName = getStr(body, "firstName");
    var lastName = getStr(body, "lastName");
    if (username == null || username.isBlank()
        || email == null || email.isBlank()
        || password == null || password.isBlank()
        || firstName == null || firstName.isBlank()) {
      return ResponseEntity.badRequest().body(ApiResponse.error("Chybí povinná pole"));
    }
    if (lastName == null) lastName = "";
    if (password.length() < 6) {
      return ResponseEntity.badRequest().body(ApiResponse.error("Heslo musí mít alespoň 6 znaků"));
    }
    if (userRepository.findByUsername(username).isPresent()) {
      return ResponseEntity.badRequest().body(ApiResponse.error("Uživatelské jméno již existuje"));
    }
    if (userRepository.findByEmail(email).isPresent()) {
      return ResponseEntity.badRequest().body(ApiResponse.error("Email již existuje"));
    }
    var roleStr = getStr(body, "role");
    var userRole = UserRole.RACER;
    if (roleStr != null) {
      try { userRole = UserRole.valueOf(roleStr.toUpperCase()); } catch (IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(ApiResponse.error("Neplatná role: " + roleStr));
      }
    }
    var user = new User(email, username, passwordEncoder.encode(password), userRole, firstName, lastName, java.time.Instant.now());
    var phone = getStr(body, "phone");
    if (phone != null) user.setPhone(phone);
    var memberSinceStr = getStr(body, "memberSince");
    if (memberSinceStr != null && !memberSinceStr.isBlank()) {
      user.setMemberSince(LocalDate.parse(memberSinceStr));
    }
    var appRoleIdsRaw = body.get("appRoleIds");
    if (appRoleIdsRaw instanceof List<?> ids) {
      for (var idObj : ids) {
        if (idObj instanceof Number num) {
          appRoleRepository.findById(num.longValue()).ifPresent(role -> user.getAppRoles().add(role));
        }
      }
    }
    userRepository.save(user);
    return ResponseEntity.ok(ApiResponse.ok(Map.of("id", user.getId(), "message", "Uživatel vytvořen")));
  }

  private static String getStr(Map<String, Object> map, String key) {
    var v = map.get(key);
    return v instanceof String s ? s : null;
  }

  @DeleteMapping("/{id}")
  @Transactional
  public ResponseEntity<?> delete(@PathVariable Long id) {
    var userOpt = userRepository.findById(id);
    if (userOpt.isEmpty()) return ResponseEntity.notFound().build();
    userRepository.delete(userOpt.get());
    return ResponseEntity.noContent().build();
  }

  @PutMapping("/{id}/password")
  @Transactional
  public ResponseEntity<?> setPassword(@PathVariable Long id, @RequestBody Map<String, String> body) {
    var userOpt = userRepository.findById(id);
    if (userOpt.isEmpty()) return ResponseEntity.notFound().build();
    var newPassword = body.get("newPassword");
    if (newPassword == null || newPassword.isBlank() || newPassword.length() < 6) {
      return ResponseEntity.badRequest().body(ApiResponse.error("Heslo musí mít alespoň 6 znaků"));
    }
    userOpt.get().setPassword(passwordEncoder.encode(newPassword));
    userRepository.save(userOpt.get());
    return ResponseEntity.ok(ApiResponse.ok(Map.of("message", "Heslo změněno")));
  }

  @PostMapping("/{userId}/roles")
  @Transactional
  public ResponseEntity<?> addRole(@PathVariable Long userId, @RequestBody Map<String, Object> body) {
    var user = userRepository.findById(userId);
    if (user.isEmpty()) return ResponseEntity.notFound().build();
    var roleId = body.get("roleId");
    if (roleId == null) return ResponseEntity.badRequest().body(ApiResponse.error("Chybí roleId"));
    var role = appRoleRepository.findById(((Number) roleId).longValue());
    if (role.isEmpty()) return ResponseEntity.badRequest().body(ApiResponse.error("Role neexistuje"));
    user.get().getAppRoles().add(role.get());
    userRepository.save(user.get());
    return ResponseEntity.ok(ApiResponse.ok(Map.of("message", "Role přidána")));
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

  @PostMapping("/{id}/impersonate")
  public ResponseEntity<?> impersonate(@PathVariable Long id) {
    var userOpt = userRepository.findById(id);
    if (userOpt.isEmpty()) return ResponseEntity.badRequest().body(ApiResponse.error("Uživatel nenalezen"));
    var user = userOpt.get();
    String accessToken = jwtService.generateAccessToken(user);
    String roleStr = user.getAppRoles().isEmpty()
        ? user.getRole().name()
        : user.getAppRoles().stream().map(AppRole::getName).collect(Collectors.joining(","));
    return ResponseEntity.ok(ApiResponse.ok(Map.of("accessToken", accessToken, "username", user.getUsername(), "name", user.getName(), "role", roleStr)));
  }
}
