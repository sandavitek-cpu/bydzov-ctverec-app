package cz.previt.bydzovctverec.web;

import cz.previt.bydzovctverec.domain.User;
import cz.previt.bydzovctverec.domain.UserRepository;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/account")
public class AccountController {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  public AccountController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @GetMapping
  public ResponseEntity<?> get(Authentication authentication) {
    var u = (User) authentication.getPrincipal();
    var phone = u.getPhone() != null ? u.getPhone() : "";
    var memberSince = u.getMemberSince() != null ? u.getMemberSince().toString() : "";
    return ResponseEntity.ok(Map.of(
        "id", u.getId(),
        "email", u.getEmail(),
        "username", u.getUsername(),
        "firstName", u.getFirstName(),
        "lastName", u.getLastName(),
        "name", u.getName(),
        "phone", phone,
        "memberSince", memberSince,
        "role", u.getRole().name()));
  }

  @PutMapping("/password")
  @Transactional
  public ResponseEntity<?> changePassword(Authentication authentication,
      @RequestBody Map<String, String> body) {
    var u = (User) authentication.getPrincipal();
    var currentPassword = body.get("currentPassword");
    var newPassword = body.get("newPassword");
    if (currentPassword == null || newPassword == null || newPassword.isBlank()) {
      return ResponseEntity.badRequest().body(Map.of("error", "Chybí heslo"));
    }
    if (newPassword.length() < 6) {
      return ResponseEntity.badRequest().body(Map.of("error", "Nové heslo musí mít alespoň 6 znaků"));
    }
    if (!passwordEncoder.matches(currentPassword, u.getPassword())) {
      return ResponseEntity.status(401).body(Map.of("error", "Současné heslo nesouhlasí"));
    }
    u.setPassword(passwordEncoder.encode(newPassword));
    userRepository.save(u);
    return ResponseEntity.ok(Map.of("message", "Heslo změněno"));
  }

  @PutMapping
  @Transactional
  public ResponseEntity<?> update(Authentication authentication,
      @RequestBody Map<String, String> body) {
    var u = (User) authentication.getPrincipal();
    var firstName = body.get("firstName");
    var lastName = body.get("lastName");
    var email = body.get("email");
    var phone = body.get("phone");
    if (firstName != null && !firstName.isBlank()) u.setFirstName(firstName);
    if (lastName != null && !lastName.isBlank()) u.setLastName(lastName);
    if (email != null && !email.isBlank()) {
      if (!email.equals(u.getEmail()) && userRepository.findByEmail(email).isPresent()) {
        return ResponseEntity.badRequest().body(Map.of("error", "Email již existuje"));
      }
      u.setEmail(email);
    }
    if (phone != null) u.setPhone(phone);
    userRepository.save(u);
    var respPhone = u.getPhone() != null ? u.getPhone() : "";
    var memberSince = u.getMemberSince() != null ? u.getMemberSince().toString() : "";
    return ResponseEntity.ok(Map.of(
        "id", u.getId(),
        "email", u.getEmail(),
        "username", u.getUsername(),
        "firstName", u.getFirstName(),
        "lastName", u.getLastName(),
        "name", u.getName(),
        "phone", respPhone,
        "memberSince", memberSince,
        "role", u.getRole().name()));
  }
}
