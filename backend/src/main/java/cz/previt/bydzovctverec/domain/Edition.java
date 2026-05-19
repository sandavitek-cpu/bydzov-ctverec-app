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

  @Column(name = "cancellation_deadline")
  private Instant cancellationDeadline;

  @Column(name = "tow_phone", length = 30)
  private String towPhone;

  @Column(name = "tow_note", length = 500)
  private String towNote;

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

  public Instant getCancellationDeadline() { return cancellationDeadline; }

  public String getTowPhone() { return towPhone; }
  public String getTowNote() { return towNote; }

  public void setCancellationDeadline(Instant cancellationDeadline) { this.cancellationDeadline = cancellationDeadline; }
  public void setTowPhone(String towPhone) { this.towPhone = towPhone; }
  public void setTowNote(String towNote) { this.towNote = towNote; }
}
