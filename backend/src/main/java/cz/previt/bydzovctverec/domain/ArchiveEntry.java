package cz.previt.bydzovctverec.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "archive_entry")
public class ArchiveEntry {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "edition_year", nullable = false)
  private Integer editionYear;

  @Column(nullable = false)
  private Integer rank;

  @Column(name = "racer_name", nullable = false, length = 200)
  private String racerName;

  @Column(length = 200)
  private String vehicle;

  @Column(nullable = false)
  private Integer points;

  protected ArchiveEntry() {}

  public ArchiveEntry(Integer editionYear, Integer rank, String racerName, String vehicle, Integer points) {
    this.editionYear = editionYear;
    this.rank = rank;
    this.racerName = racerName;
    this.vehicle = vehicle;
    this.points = points;
  }

  public Long getId() { return id; }
  public Integer getEditionYear() { return editionYear; }
  public Integer getRank() { return rank; }
  public String getRacerName() { return racerName; }
  public String getVehicle() { return vehicle; }
  public Integer getPoints() { return points; }
}
