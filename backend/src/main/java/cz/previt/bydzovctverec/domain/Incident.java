package cz.previt.bydzovctverec.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "incident")
public class Incident {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String title;

  @Column(columnDefinition = "TEXT")
  private String description;

  @Column(name = "due_date")
  private LocalDate dueDate;

  @Column(nullable = false, length = 20)
  private String status = "OPEN";

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by_id", nullable = false)
  private User createdBy;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @OneToMany(mappedBy = "incident", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<IncidentAssignee> assignees = new ArrayList<>();

  protected Incident() {}

  public Incident(String title, String description, LocalDate dueDate, User createdBy) {
    this.title = title;
    this.description = description;
    this.dueDate = dueDate;
    this.createdBy = createdBy;
    this.status = "OPEN";
    this.createdAt = Instant.now();
    this.updatedAt = this.createdAt;
  }

  public Long getId() { return id; }
  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }
  public String getDescription() { return description; }
  public void setDescription(String description) { this.description = description; }
  public LocalDate getDueDate() { return dueDate; }
  public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public User getCreatedBy() { return createdBy; }
  public Instant getCreatedAt() { return createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
  public List<IncidentAssignee> getAssignees() { return assignees; }
  public void setAssignees(List<IncidentAssignee> assignees) { this.assignees = assignees; }
}
