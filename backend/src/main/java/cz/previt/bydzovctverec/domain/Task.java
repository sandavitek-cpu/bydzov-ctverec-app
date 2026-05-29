package cz.previt.bydzovctverec.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "task")
public class Task {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 200)
  private String name;

  @Column(columnDefinition = "TEXT")
  private String description;

  @Column(name = "recommended_points")
  private Integer recommendedPoints;

  @Column(columnDefinition = "TEXT")
  private String tools;

  @Column(name = "created_at")
  private LocalDateTime createdAt;

  protected Task() {}

  public Task(String name) {
    this.name = name;
    this.createdAt = LocalDateTime.now();
  }

  public Long getId() { return id; }
  public String getName() { return name; }
  public String getDescription() { return description; }
  public Integer getRecommendedPoints() { return recommendedPoints; }
  public String getTools() { return tools; }
  public LocalDateTime getCreatedAt() { return createdAt; }

  public void setName(String name) { this.name = name; }
  public void setDescription(String description) { this.description = description; }
  public void setRecommendedPoints(Integer recommendedPoints) { this.recommendedPoints = recommendedPoints; }
  public void setTools(String tools) { this.tools = tools; }
}
