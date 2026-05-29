package cz.previt.bydzovctverec.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(name = "incident_assignee",
    uniqueConstraints = @UniqueConstraint(columnNames = {"incident_id", "user_id"}))
public class IncidentAssignee {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "incident_id", nullable = false)
  private Incident incident;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(nullable = false, length = 20)
  private String status = "PENDING";

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected IncidentAssignee() {}

  public IncidentAssignee(Incident incident, User user) {
    this.incident = incident;
    this.user = user;
    this.status = "PENDING";
    this.createdAt = Instant.now();
  }

  public Long getId() { return id; }
  public Incident getIncident() { return incident; }
  public User getUser() { return user; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public Instant getCreatedAt() { return createdAt; }
}
