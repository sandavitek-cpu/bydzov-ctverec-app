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
@Table(name = "route_point")
public class RoutePoint {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "route_id", nullable = false)
  private Route route;

  @Column(name = "sort_order")
  private Integer sortOrder;

  @Column(nullable = false)
  private Double lat;

  @Column(nullable = false)
  private Double lng;

  @Column(name = "distance_from_start")
  private Double distanceFromStart;

  protected RoutePoint() {}

  public RoutePoint(Route route, Integer sortOrder, Double lat, Double lng) {
    this.route = route;
    this.sortOrder = sortOrder;
    this.lat = lat;
    this.lng = lng;
    this.distanceFromStart = 0.0;
  }

  public Long getId() { return id; }
  public Route getRoute() { return route; }
  public Integer getSortOrder() { return sortOrder; }
  public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
  public Double getLat() { return lat; }
  public void setLat(Double lat) { this.lat = lat; }
  public Double getLng() { return lng; }
  public void setLng(Double lng) { this.lng = lng; }
  public Double getDistanceFromStart() { return distanceFromStart; }
  public void setDistanceFromStart(Double distanceFromStart) { this.distanceFromStart = distanceFromStart; }
}
