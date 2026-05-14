package cz.previt.bydzovctverec.config;

import cz.previt.bydzovctverec.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtService {

  private final SecretKey key;

  public JwtService(@Value("${jwt.secret}") String secret) {
    this.key = Keys.hmacShaKeyFor(secret.getBytes());
  }

  public String generateAccessToken(User user) {
    return Jwts.builder()
        .subject(user.getId().toString())
        .claim("role", user.getRole().name())
        .claim("type", "access")
        .issuedAt(Date.from(Instant.now()))
        .expiration(Date.from(Instant.now().plus(24, ChronoUnit.HOURS)))
        .signWith(key)
        .compact();
  }

  public String generateRefreshToken(User user) {
    return Jwts.builder()
        .subject(user.getId().toString())
        .claim("type", "refresh")
        .issuedAt(Date.from(Instant.now()))
        .expiration(Date.from(Instant.now().plus(7, ChronoUnit.DAYS)))
        .signWith(key)
        .compact();
  }

  public Claims validate(String token) {
    return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
  }

  public Long getUserId(String token) {
    return Long.valueOf(validate(token).getSubject());
  }

  public String getRole(String token) {
    return validate(token).get("role", String.class);
  }

  public boolean isExpired(String token) {
    try {
      validate(token);
      return false;
    } catch (ExpiredJwtException e) {
      return true;
    } catch (JwtException e) {
      return true;
    }
  }
}
