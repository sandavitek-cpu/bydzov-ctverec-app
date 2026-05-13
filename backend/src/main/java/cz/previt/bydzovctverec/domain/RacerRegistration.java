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
@Table(name = "racer_registration")
public class RacerRegistration {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "edition_id", nullable = false)
  private Edition edition;

  @Column(name = "first_name", nullable = false, length = 100)
  private String firstName;

  @Column(name = "last_name", nullable = false, length = 100)
  private String lastName;

  @Column(nullable = false)
  private String email;

  @Column(name = "vehicle_description", length = 500)
  private String vehicleDescription;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected RacerRegistration() {}

  public RacerRegistration(
      Edition edition,
      String firstName,
      String lastName,
      String email,
      String vehicleDescription,
      Instant createdAt) {
    this.edition = edition;
    this.firstName = firstName;
    this.lastName = lastName;
    this.email = email;
    this.vehicleDescription = vehicleDescription;
    this.createdAt = createdAt;
  }

  public Long getId() {
    return id;
  }

  public Edition getEdition() {
    return edition;
  }

  public String getFirstName() {
    return firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public String getEmail() {
    return email;
  }

  public String getVehicleDescription() {
    return vehicleDescription;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
