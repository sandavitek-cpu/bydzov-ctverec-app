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
}
