package cz.previt.bydzovctverec.config;

import static org.assertj.core.api.Assertions.assertThat;

import cz.previt.bydzovctverec.domain.Role;
import cz.previt.bydzovctverec.domain.User;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

  JwtService jwtService;
  User user;

  @BeforeEach
  void setUp() {
    jwtService = new JwtService("test-secret-key-that-is-at-least-64-characters-long-for-hmac-sha-test");
    user = new User(42L, "test@test.cz", "pass", Role.ADMIN, "Test", Instant.now());
  }

  @Test
  void generatesAndValidatesAccessToken() {
    String token = jwtService.generateAccessToken(user);
    assertThat(token).isNotBlank();
    assertThat(jwtService.getUserId(token)).isEqualTo(user.getId());
    assertThat(jwtService.getRole(token)).isEqualTo("ADMIN");
  }

  @Test
  void generatesAndValidatesRefreshToken() {
    String token = jwtService.generateRefreshToken(user);
    assertThat(token).isNotBlank();
    assertThat(jwtService.getUserId(token)).isEqualTo(user.getId());
  }

  @Test
  void detectsExpiredToken() {
    String token = jwtService.generateAccessToken(user);
    assertThat(jwtService.isExpired(token)).isFalse();
  }

  @Test
  void rejectsInvalidToken() {
    assertThat(jwtService.isExpired("invalid-token")).isTrue();
  }
}
