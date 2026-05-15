package cz.previt.bydzovctverec.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "changelog")
public class ChangeLogEntry {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 50)
  private String version;

  @Column(nullable = false, length = 500)
  private String description;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected ChangeLogEntry() {}

  public ChangeLogEntry(String version, String description, Instant createdAt) {
    this.version = version;
    this.description = description;
    this.createdAt = createdAt;
  }

  public Long getId() { return id; }
  public String getVersion() { return version; }
  public String getDescription() { return description; }
  public Instant getCreatedAt() { return createdAt; }
}
