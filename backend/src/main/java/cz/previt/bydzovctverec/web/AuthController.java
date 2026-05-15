package cz.previt.bydzovctverec.web;

import cz.previt.bydzovctverec.config.JwtService;
import cz.previt.bydzovctverec.domain.AppRole;
import cz.previt.bydzovctverec.domain.User;
import cz.previt.bydzovctverec.domain.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final UserDetailsService userDetailsService;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final UserRepository userRepository;

  public AuthController(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder, JwtService jwtService, UserRepository userRepository) {
    this.userDetailsService = userDetailsService;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
    this.userRepository = userRepository;
  }

  @PostMapping("/login")
  public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
    try {
      var userDetails = userDetailsService.loadUserByUsername(request.email());
      if (!passwordEncoder.matches(request.password(), userDetails.getPassword())) {
        return ResponseEntity.status(401).build();
      }
    } catch (Exception e) {
      return ResponseEntity.status(401).build();
    }
    User user = userRepository.findByEmail(request.email()).orElseThrow();
    String accessToken = jwtService.generateAccessToken(user);
    String refreshToken = jwtService.generateRefreshToken(user);
    var roleStr = user.getAppRoles().isEmpty()
        ? user.getRole().name()
        : user.getAppRoles().stream().map(AppRole::getName).collect(java.util.stream.Collectors.joining(","));
    return ResponseEntity.ok(new LoginResponse(accessToken, refreshToken, roleStr, user.getName()));
  }

  @PostMapping("/refresh")
  public ResponseEntity<RefreshResponse> refresh(@Valid @RequestBody RefreshRequest request) {
    if (jwtService.isExpired(request.refreshToken())) {
      return ResponseEntity.status(401).build();
    }
    Long userId = jwtService.getUserId(request.refreshToken());
    User user = userRepository.findById(userId).orElse(null);
    if (user == null) {
      return ResponseEntity.status(401).build();
    }
    String accessToken = jwtService.generateAccessToken(user);
    return ResponseEntity.ok(new RefreshResponse(accessToken));
  }

  public record LoginRequest(@Email @NotBlank String email, @NotBlank String password) {}
  public record LoginResponse(String accessToken, String refreshToken, String role, String name) {}
  public record RefreshRequest(@NotBlank String refreshToken) {}
  public record RefreshResponse(String accessToken) {}
}
