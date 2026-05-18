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
@Table(name = "race_category")
public class RaceCategory {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "edition_id", nullable = false)
  private Edition edition;

  @Column(nullable = false, length = 200)
  private String name;

  @Column(length = 50)
  private String code;

  @Column(length = 20)
  private String variant;

  @Column(nullable = false, length = 30)
  private String determination;

  @Column(name = "sort_order")
  private Integer sortOrder;

  @Column(name = "winner_registration_id")
  private Long winnerRegistrationId;

  @Column(name = "winner_name", length = 200)
  private String winnerName;

  @Column(name = "winner_team", length = 200)
  private String winnerTeam;

  @Column(name = "winner_number")
  private Integer winnerNumber;

  @Column(name = "winner_points")
  private Integer winnerPoints;

  protected RaceCategory() {}

  public RaceCategory(Edition edition, String name, String code, String variant,
      String determination, Integer sortOrder) {
    this.edition = edition;
    this.name = name;
    this.code = code;
    this.variant = variant;
    this.determination = determination;
    this.sortOrder = sortOrder;
  }

  public Long getId() { return id; }

  public Edition getEdition() { return edition; }

  public String getName() { return name; }
  public void setName(String name) { this.name = name; }

  public String getCode() { return code; }
  public void setCode(String code) { this.code = code; }

  public String getVariant() { return variant; }
  public void setVariant(String variant) { this.variant = variant; }

  public String getDetermination() { return determination; }
  public void setDetermination(String determination) { this.determination = determination; }

  public Integer getSortOrder() { return sortOrder; }
  public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }

  public Long getWinnerRegistrationId() { return winnerRegistrationId; }
  public void setWinnerRegistrationId(Long id) { this.winnerRegistrationId = id; }

  public String getWinnerName() { return winnerName; }
  public void setWinnerName(String name) { this.winnerName = name; }

  public String getWinnerTeam() { return winnerTeam; }
  public void setWinnerTeam(String team) { this.winnerTeam = team; }

  public Integer getWinnerNumber() { return winnerNumber; }
  public void setWinnerNumber(Integer n) { this.winnerNumber = n; }

  public Integer getWinnerPoints() { return winnerPoints; }
  public void setWinnerPoints(Integer p) { this.winnerPoints = p; }
}
