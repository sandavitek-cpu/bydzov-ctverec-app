package cz.previt.bydzovctverec.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "edition")
public class Edition {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "edition_year", nullable = false, unique = true)
  private Integer editionYear;

  @Column
  private String label;

  @Column(name = "race_started_at")
  private Instant raceStartedAt;

  @Column(name = "race_finished_at")
  private Instant raceFinishedAt;

  protected Edition() {}

  public Edition(Integer editionYear, String label) {
    this.editionYear = editionYear;
    this.label = label;
  }

  public Long getId() { return id; }

  public Integer getEditionYear() { return editionYear; }

  public String getLabel() { return label; }

  public Instant getRaceStartedAt() { return raceStartedAt; }

  public void setRaceStartedAt(Instant raceStartedAt) { this.raceStartedAt = raceStartedAt; }

  public Instant getRaceFinishedAt() { return raceFinishedAt; }

  public void setRaceFinishedAt(Instant raceFinishedAt) { this.raceFinishedAt = raceFinishedAt; }
}
