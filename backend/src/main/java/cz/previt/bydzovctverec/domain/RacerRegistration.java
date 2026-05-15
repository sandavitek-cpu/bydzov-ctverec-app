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

  @Column(length = 20)
  private String variant;

  @Column(name = "vehicle_make", length = 200)
  private String vehicleMake;

  @Column(name = "first_time")
  private Boolean firstTime;

  @Column(length = 10)
  private String gender;

  @Column(name = "driver_age")
  private Integer driverAge;

  @Column(length = 200)
  private String club;

  @Column(length = 300)
  private String address;

  @Column(name = "youngest_age")
  private Integer youngestAge;

  @Column(name = "youngest_name", length = 200)
  private String youngestName;

  @Column(name = "engine_displacement")
  private Integer engineDisplacement;

  @Column(name = "power")
  private Integer power;

  @Column(name = "max_speed")
  private Integer maxSpeed;

  @Column(name = "vehicle_notes", length = 500)
  private String vehicleNotes;

  @Column(length = 500)
  private String notes;

  @Column
  private Boolean contacted;

  @Column(name = "properly_registered")
  private Boolean properlyRegistered;

  @Column
  private Boolean arrived;

  @Column
  private Boolean consent;

  @Column
  private Boolean approved;

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

  public RacerRegistration(
      Edition edition, String teamName, String email, String phone,
      String vehicleCategory, String vehicleMake, String vehiclePlate,
      Integer vehicleYear, Integer crewCount, Integer startNumber,
      Integer startFee, String variant, String firstName, String lastName,
      Boolean firstTime, String gender, Integer driverAge, String club,
      String address, Integer youngestAge, String youngestName,
      Integer engineDisplacement, Integer power, Integer maxSpeed,
      String vehicleNotes, String notes, Boolean contacted,
      Boolean properlyRegistered, Boolean arrived, Boolean consent, Boolean approved,
      Instant createdAt) {
    this.edition = edition;
    this.teamName = teamName;
    this.email = email;
    this.phone = phone;
    this.vehicleCategory = vehicleCategory;
    this.vehicleMake = vehicleMake;
    this.vehiclePlate = vehiclePlate;
    this.vehicleYear = vehicleYear;
    this.crewCount = crewCount;
    this.startNumber = startNumber;
    this.startFee = startFee;
    this.variant = variant;
    this.firstName = firstName;
    this.lastName = lastName;
    this.firstTime = firstTime;
    this.gender = gender;
    this.driverAge = driverAge;
    this.club = club;
    this.address = address;
    this.youngestAge = youngestAge;
    this.youngestName = youngestName;
    this.engineDisplacement = engineDisplacement;
    this.power = power;
    this.maxSpeed = maxSpeed;
    this.vehicleNotes = vehicleNotes;
    this.notes = notes;
    this.contacted = contacted;
    this.properlyRegistered = properlyRegistered;
    this.arrived = arrived;
    this.consent = consent;
    this.approved = approved;
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

  public String getVariant() { return variant; }

  public String getVehicleMake() { return vehicleMake; }

  public Boolean getFirstTime() { return firstTime; }

  public String getGender() { return gender; }

  public Integer getDriverAge() { return driverAge; }

  public String getClub() { return club; }

  public String getAddress() { return address; }

  public Integer getYoungestAge() { return youngestAge; }

  public String getYoungestName() { return youngestName; }

  public Integer getEngineDisplacement() { return engineDisplacement; }

  public Integer getPower() { return power; }

  public Integer getMaxSpeed() { return maxSpeed; }

  public String getVehicleNotes() { return vehicleNotes; }

  public String getNotes() { return notes; }

  public Boolean getContacted() { return contacted; }

  public Boolean getProperlyRegistered() { return properlyRegistered; }

  public Boolean getArrived() { return arrived; }

  public Boolean getConsent() { return consent; }

  public void setVariant(String variant) { this.variant = variant; }

  public void setVehicleMake(String vehicleMake) { this.vehicleMake = vehicleMake; }

  public void setFirstTime(Boolean firstTime) { this.firstTime = firstTime; }

  public void setGender(String gender) { this.gender = gender; }

  public void setDriverAge(Integer driverAge) { this.driverAge = driverAge; }

  public void setClub(String club) { this.club = club; }

  public void setAddress(String address) { this.address = address; }

  public void setYoungestAge(Integer youngestAge) { this.youngestAge = youngestAge; }

  public void setYoungestName(String youngestName) { this.youngestName = youngestName; }

  public void setEngineDisplacement(Integer engineDisplacement) { this.engineDisplacement = engineDisplacement; }

  public void setPower(Integer power) { this.power = power; }

  public void setMaxSpeed(Integer maxSpeed) { this.maxSpeed = maxSpeed; }

  public void setVehicleNotes(String vehicleNotes) { this.vehicleNotes = vehicleNotes; }

  public void setNotes(String notes) { this.notes = notes; }

  public void setContacted(Boolean contacted) { this.contacted = contacted; }

  public void setProperlyRegistered(Boolean properlyRegistered) { this.properlyRegistered = properlyRegistered; }

  public void setArrived(Boolean arrived) { this.arrived = arrived; }

  public void setConsent(Boolean consent) { this.consent = consent; }

  public Boolean getApproved() { return approved; }
  public void setApproved(Boolean approved) { this.approved = approved; }
}
