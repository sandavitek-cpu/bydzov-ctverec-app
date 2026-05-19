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
import java.time.Instant;

@Entity
@Table(name = "route")
public class Route {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "edition_id", nullable = false)
  private Edition edition;

  @Column(length = 30)
  private String variant;

  @Column(length = 200)
  private String name;

  @Column(name = "total_distance")
  private Double totalDistance;

  @Column(name = "avg_speed_kmph", nullable = false)
  private Integer avgSpeedKmph = 30;

  @Column
  private Boolean published;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected Route() {}

  public Route(Edition edition, String variant, String name, Instant createdAt) {
    this.edition = edition;
    this.variant = variant;
    this.name = name;
    this.totalDistance = 0.0;
    this.avgSpeedKmph = 30;
    this.published = false;
    this.createdAt = createdAt;
  }

  public Long getId() { return id; }
  public Edition getEdition() { return edition; }
  public String getVariant() { return variant; }
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  public Double getTotalDistance() { return totalDistance; }
  public void setTotalDistance(Double totalDistance) { this.totalDistance = totalDistance; }
  public Integer getAvgSpeedKmph() { return avgSpeedKmph; }
  public void setAvgSpeedKmph(Integer avgSpeedKmph) { this.avgSpeedKmph = avgSpeedKmph; }
  public Boolean getPublished() { return published; }
  public void setPublished(Boolean published) { this.published = published; }
  public Instant getCreatedAt() { return createdAt; }
}
