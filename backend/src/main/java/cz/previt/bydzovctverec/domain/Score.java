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
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(name = "score", uniqueConstraints = @UniqueConstraint(columnNames = {"racer_registration_id", "checkpoint_id"}))
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

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "checkpoint_id", nullable = false)
  private Checkpoint checkpoint;

  @Column(nullable = false)
  private Integer points;

  @Column(length = 500)
  private String note;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected Score() {}

  public Score(RacerRegistration racerRegistration, User judge, Checkpoint checkpoint, Integer points, String note, Instant createdAt) {
    this.racerRegistration = racerRegistration;
    this.judge = judge;
    this.checkpoint = checkpoint;
    this.points = points;
    this.note = note;
    this.createdAt = createdAt;
  }

  public Long getId() { return id; }
  public RacerRegistration getRacerRegistration() { return racerRegistration; }
  public User getJudge() { return judge; }
  public Checkpoint getCheckpoint() { return checkpoint; }
  public Integer getPoints() { return points; }
  public String getNote() { return note; }
  public Instant getCreatedAt() { return createdAt; }
}
