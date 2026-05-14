package cz.previt.bydzovctverec.web;

import cz.previt.bydzovctverec.config.JwtService;
import cz.previt.bydzovctverec.domain.User;
import cz.previt.bydzovctverec.domain.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final AuthenticationManager authenticationManager;
  private final JwtService jwtService;
  private final UserRepository userRepository;

  public AuthController(AuthenticationManager authenticationManager, JwtService jwtService, UserRepository userRepository) {
    this.authenticationManager = authenticationManager;
    this.jwtService = jwtService;
    this.userRepository = userRepository;
  }

  @GetMapping("/ping")
  public ResponseEntity<String> ping() {
    return ResponseEntity.ok("pong");
  }

  @PostMapping("/login")
  public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
    var auth = authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(request.email(), request.password()));
    String userEmail = auth.getName();
    User user = userRepository.findByEmail(userEmail).orElseThrow();
    String accessToken = jwtService.generateAccessToken(user);
    String refreshToken = jwtService.generateRefreshToken(user);
    return ResponseEntity.ok(new LoginResponse(accessToken, refreshToken, user.getRole().name(), user.getName()));
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
