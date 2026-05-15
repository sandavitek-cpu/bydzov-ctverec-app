package cz.previt.bydzovctverec.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "app_role")
public class AppRole {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 50)
  private String name;

  @Column(name = "display_name", length = 200)
  private String displayName;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected AppRole() {}

  public AppRole(String name, String displayName, Instant createdAt) {
    this.name = name;
    this.displayName = displayName;
    this.createdAt = createdAt;
  }

  public Long getId() { return id; }
  public String getName() { return name; }
  public String getDisplayName() { return displayName; }
  public void setDisplayName(String displayName) { this.displayName = displayName; }
  public Instant getCreatedAt() { return createdAt; }
}
