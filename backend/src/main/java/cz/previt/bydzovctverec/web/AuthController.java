package cz.previt.bydzovctverec.web;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import cz.previt.bydzovctverec.config.JwtService;
import cz.previt.bydzovctverec.domain.AppRole;
import cz.previt.bydzovctverec.domain.AppRoleRepository;
import cz.previt.bydzovctverec.domain.User;
import cz.previt.bydzovctverec.domain.UserRepository;
import cz.previt.bydzovctverec.domain.UserRole;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.Collections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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

  private static final Logger log = LoggerFactory.getLogger(AuthController.class);
  private final UserDetailsService userDetailsService;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final UserRepository userRepository;
  private final AppRoleRepository appRoleRepository;
  private final String googleClientId;

  public AuthController(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder,
      JwtService jwtService, UserRepository userRepository, AppRoleRepository appRoleRepository,
      @Value("${google.client-id}") String googleClientId) {
    this.userDetailsService = userDetailsService;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
    this.userRepository = userRepository;
    this.appRoleRepository = appRoleRepository;
    this.googleClientId = googleClientId;
  }

  @PostMapping("/login")
  public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
    try {
      String identity = request.login().trim();
      var userDetails = userDetailsService.loadUserByUsername(identity);
      if (!passwordEncoder.matches(request.password(), userDetails.getPassword())) {
        return ResponseEntity.status(401).body(ApiResponse.error("Neplatné heslo"));
      }
      User user = userRepository.findByUsername(identity)
          .orElseGet(() -> userRepository.findByEmail(identity).orElse(null));
      if (user == null) {
        return ResponseEntity.status(401).body(ApiResponse.error("Uživatel nenalezen"));
      }
      return ResponseEntity.ok(buildLoginResponse(user));
    } catch (Exception e) {
      log.warn("Login failed for {}: {}", request.login(), e.getMessage());
      return ResponseEntity.status(401).body(ApiResponse.error("Přihlášení selhalo"));
    }
  }

  @PostMapping("/refresh")
  public ResponseEntity<?> refresh(@Valid @RequestBody RefreshRequest request) {
    try {
      var claims = jwtService.validate(request.refreshToken());
      if (!"refresh".equals(claims.get("type", String.class))) {
        return ResponseEntity.status(401).body(ApiResponse.error("Token není refresh token"));
      }
      Long userId = Long.valueOf(claims.getSubject());
      User user = userRepository.findById(userId).orElse(null);
      if (user == null) {
        return ResponseEntity.status(401).body(ApiResponse.error("Uživatel nenalezen"));
      }
      String newAccessToken = jwtService.generateAccessToken(user);
      return ResponseEntity.ok(new RefreshResponse(newAccessToken));
    } catch (Exception e) {
      log.warn("Refresh failed: {}", e.getMessage());
      return ResponseEntity.status(401).body(ApiResponse.error("Refresh token je neplatný nebo expirovaný"));
    }
  }

  @PostMapping("/google")
  public ResponseEntity<?> googleLogin(@RequestBody GoogleRequest request) {
    try {
      if (googleClientId == null || googleClientId.isBlank()) {
        return ResponseEntity.status(500).body(ApiResponse.error("Google přihlášení není nakonfigurováno"));
      }
      GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
          .setAudience(Collections.singletonList(googleClientId))
          .build();
      GoogleIdToken idToken = verifier.verify(request.credential());
      if (idToken == null) {
        return ResponseEntity.status(401).body(ApiResponse.error("Neplatný Google token"));
      }
      GoogleIdToken.Payload payload = idToken.getPayload();
      String email = payload.getEmail();
      String givenName = (String) payload.get("given_name");
      String familyName = (String) payload.get("family_name");

      User user = userRepository.findByEmail(email).orElse(null);
      if (user == null) {
        String firstName = givenName != null ? givenName : email;
        String lastName = familyName != null ? familyName : "";
        var racerRole = appRoleRepository.findByName("RACER").orElse(null);
        user = new User(email, email, passwordEncoder.encode(""), UserRole.RACER, firstName, lastName, Instant.now());
        if (racerRole != null) user.getAppRoles().add(racerRole);
        userRepository.save(user);
        log.info("User created via Google: {}", email);
      }
      return ResponseEntity.ok(buildLoginResponse(user));
    } catch (Exception e) {
      log.warn("Google login failed: {}", e.getMessage());
      return ResponseEntity.status(401).body(ApiResponse.error("Google přihlášení selhalo"));
    }
  }

  private LoginResponse buildLoginResponse(User user) {
    String accessToken = jwtService.generateAccessToken(user);
    String refreshToken = jwtService.generateRefreshToken(user);
    var roleParts = new java.util.ArrayList<String>();
    roleParts.add(user.getRole().name());
    user.getAppRoles().stream().map(AppRole::getName)
        .filter(n -> !n.equals(user.getRole().name()))
        .forEach(roleParts::add);
    var roleStr = String.join(",", roleParts);
    return new LoginResponse(accessToken, refreshToken, roleStr, user.getName(), user.getUsername());
  }

  public record LoginRequest(@NotBlank String login, @NotBlank String password) {}
  public record LoginResponse(String accessToken, String refreshToken, String role, String name, String username) {}
  public record RefreshRequest(@NotBlank String refreshToken) {}
  public record RefreshResponse(String accessToken) {}
  public record GoogleRequest(@NotBlank String credential) {}
}
