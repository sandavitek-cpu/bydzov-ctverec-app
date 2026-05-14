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

  @Column(name = "first_name", length = 100)
  private String firstName;

  @Column(name = "last_name", length = 100)
  private String lastName;

  @Column(nullable = false)
  private String email;

  @Column(name = "vehicle_description", length = 500)
  private String vehicleDescription;

  @Column(name = "team_name", length = 200)
  private String teamName;

  @Column(length = 30)
  private String phone;

  @Column(name = "vehicle_category", length = 50)
  private String vehicleCategory;

  @Column(name = "vehicle_plate", length = 20)
  private String vehiclePlate;

  @Column(name = "vehicle_year")
  private Integer vehicleYear;

  @Column(name = "crew_count")
  private Integer crewCount;

  @Column(name = "start_number")
  private Integer startNumber;

  @Column(name = "start_fee")
  private Integer startFee;

  @Column(length = 20)
  private String status;

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
    this.status = "PENDING";
  }

  public RacerRegistration(
      Edition edition, String teamName, String email, String phone,
      String vehicleCategory, String vehiclePlate, Integer vehicleYear,
      Integer crewCount, Integer startNumber, Integer startFee,
      Instant createdAt) {
    this.edition = edition;
    this.teamName = teamName;
    this.email = email;
    this.phone = phone;
    this.vehicleCategory = vehicleCategory;
    this.vehiclePlate = vehiclePlate;
    this.vehicleYear = vehicleYear;
    this.crewCount = crewCount;
    this.startNumber = startNumber;
    this.startFee = startFee;
    this.createdAt = createdAt;
    this.status = "PENDING";
  }

  public Long getId() { return id; }

  public Edition getEdition() { return edition; }

  public String getFirstName() { return firstName; }

  public String getLastName() { return lastName; }

  public String getEmail() { return email; }

  public String getVehicleDescription() { return vehicleDescription; }

  public String getTeamName() { return teamName; }

  public String getPhone() { return phone; }

  public String getVehicleCategory() { return vehicleCategory; }

  public String getVehiclePlate() { return vehiclePlate; }

  public Integer getVehicleYear() { return vehicleYear; }

  public Integer getCrewCount() { return crewCount; }

  public Integer getStartNumber() { return startNumber; }

  public Integer getStartFee() { return startFee; }

  public String getStatus() { return status; }

  public Instant getCreatedAt() { return createdAt; }
}
