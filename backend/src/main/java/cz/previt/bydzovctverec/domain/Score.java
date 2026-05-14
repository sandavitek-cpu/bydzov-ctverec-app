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
@Table(name = "score")
public class Score {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "racer_registration_id", nullable = false)
  private RacerRegistration racerRegistration;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "judge_id", nullable = false)
  private User judge;

  @Column(name = "run_number", nullable = false)
  private Integer runNumber;

  @Column(nullable = false)
  private Integer points;

  @Column(length = 500)
  private String note;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected Score() {}

  public Score(RacerRegistration racerRegistration, User judge, Integer runNumber, Integer points, String note, Instant createdAt) {
    this.racerRegistration = racerRegistration;
    this.judge = judge;
    this.runNumber = runNumber;
    this.points = points;
    this.note = note;
    this.createdAt = createdAt;
  }

  public Long getId() { return id; }
  public RacerRegistration getRacerRegistration() { return racerRegistration; }
  public User getJudge() { return judge; }
  public Integer getRunNumber() { return runNumber; }
  public Integer getPoints() { return points; }
  public String getNote() { return note; }
  public Instant getCreatedAt() { return createdAt; }
}
