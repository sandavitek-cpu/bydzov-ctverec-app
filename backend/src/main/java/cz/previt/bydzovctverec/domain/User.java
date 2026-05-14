package cz.previt.bydzovctverec.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "app_user")
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String email;

  @Column(nullable = false)
  private String password;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Role role;

  @Column(nullable = false)
  private String name;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected User() {}

  public User(Long id, String email, String password, Role role, String name, Instant createdAt) {
    this.id = id;
    this.email = email;
    this.password = password;
    this.role = role;
    this.name = name;
    this.createdAt = createdAt;
  }

  public User(String email, String password, Role role, String name, Instant createdAt) {
    this.email = email;
    this.password = password;
    this.role = role;
    this.name = name;
    this.createdAt = createdAt;
  }

  public Long getId() { return id; }
  public String getEmail() { return email; }
  public String getPassword() { return password; }
  public Role getRole() { return role; }
  public String getName() { return name; }
  public Instant getCreatedAt() { return createdAt; }
}
