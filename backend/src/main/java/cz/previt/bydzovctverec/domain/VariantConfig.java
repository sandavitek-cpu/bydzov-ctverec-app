package cz.previt.bydzovctverec.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "variant_config")
public class VariantConfig {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "edition_id", nullable = false)
  private Edition edition;

  @Column(name = "variant_code", nullable = false, length = 50)
  private String variantCode;

  @Column(nullable = false)
  private String label;

  @Column(name = "registration_deadline")
  private LocalDate registrationDeadline;

  @Column(name = "race_date")
  private LocalDate raceDate;

  @Column(nullable = false)
  private boolean enabled = true;

  protected VariantConfig() {}

  public VariantConfig(Edition edition, String variantCode, String label) {
    this.edition = edition;
    this.variantCode = variantCode;
    this.label = label;
  }

  public Long getId() { return id; }
  public Edition getEdition() { return edition; }
  public String getVariantCode() { return variantCode; }
  public void setVariantCode(String variantCode) { this.variantCode = variantCode; }
  public String getLabel() { return label; }
  public void setLabel(String label) { this.label = label; }
  public LocalDate getRegistrationDeadline() { return registrationDeadline; }
  public void setRegistrationDeadline(LocalDate registrationDeadline) { this.registrationDeadline = registrationDeadline; }
  public LocalDate getRaceDate() { return raceDate; }
  public void setRaceDate(LocalDate raceDate) { this.raceDate = raceDate; }
  public boolean isEnabled() { return enabled; }
  public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
