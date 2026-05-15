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

@Entity
@Table(name = "checkpoint")
public class Checkpoint {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "edition_id", nullable = false)
  private Edition edition;

  @Column(nullable = false, length = 200)
  private String name;

  @Column(nullable = false)
  private Double lat;

  @Column(nullable = false)
  private Double lng;

  @Column(nullable = false)
  private Integer radius;

  @Column(name = "sort_order")
  private Integer sortOrder;

  @Column(name = "task_description", length = 1000)
  private String taskDescription;

  @Column(name = "max_points")
  private Integer maxPoints;

  @Column(name = "volunteer_name", length = 200)
  private String volunteerName;

  protected Checkpoint() {}

  public Checkpoint(Edition edition, String name, Double lat, Double lng, Integer radius, Integer sortOrder) {
    this.edition = edition;
    this.name = name;
    this.lat = lat;
    this.lng = lng;
    this.radius = radius;
    this.sortOrder = sortOrder;
  }

  public Long getId() { return id; }
  public Edition getEdition() { return edition; }
  public String getName() { return name; }
  public Double getLat() { return lat; }
  public Double getLng() { return lng; }
  public Integer getRadius() { return radius; }
  public Integer getSortOrder() { return sortOrder; }
  public String getTaskDescription() { return taskDescription; }
  public Integer getMaxPoints() { return maxPoints; }
  public String getVolunteerName() { return volunteerName; }

  public void setName(String name) { this.name = name; }
  public void setLat(Double lat) { this.lat = lat; }
  public void setLng(Double lng) { this.lng = lng; }
  public void setRadius(Integer radius) { this.radius = radius; }
  public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
  public void setTaskDescription(String taskDescription) { this.taskDescription = taskDescription; }
  public void setMaxPoints(Integer maxPoints) { this.maxPoints = maxPoints; }
  public void setVolunteerName(String volunteerName) { this.volunteerName = volunteerName; }
}
