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
@Table(name = "vehicle")
public class Vehicle {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "vehicle_make", length = 200)
  private String vehicleMake;

  @Column(name = "vehicle_plate", length = 20)
  private String vehiclePlate;

  @Column(name = "vehicle_year")
  private Integer vehicleYear;

  @Column(name = "vehicle_category", length = 50)
  private String vehicleCategory;

  @Column(name = "engine_displacement")
  private Integer engineDisplacement;

  @Column(name = "power")
  private Integer power;

  @Column(name = "max_speed")
  private Integer maxSpeed;

  @Column(name = "vehicle_notes", length = 500)
  private String vehicleNotes;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected Vehicle() {}

  public Vehicle(User user, String vehicleMake, String vehiclePlate, Integer vehicleYear,
      String vehicleCategory, Integer engineDisplacement, Integer power, Integer maxSpeed,
      String vehicleNotes, Instant createdAt) {
    this.user = user;
    this.vehicleMake = vehicleMake;
    this.vehiclePlate = vehiclePlate;
    this.vehicleYear = vehicleYear;
    this.vehicleCategory = vehicleCategory;
    this.engineDisplacement = engineDisplacement;
    this.power = power;
    this.maxSpeed = maxSpeed;
    this.vehicleNotes = vehicleNotes;
    this.createdAt = createdAt;
  }

  public Long getId() { return id; }
  public User getUser() { return user; }
  public String getVehicleMake() { return vehicleMake; }
  public void setVehicleMake(String v) { vehicleMake = v; }
  public String getVehiclePlate() { return vehiclePlate; }
  public void setVehiclePlate(String v) { vehiclePlate = v; }
  public Integer getVehicleYear() { return vehicleYear; }
  public void setVehicleYear(Integer v) { vehicleYear = v; }
  public String getVehicleCategory() { return vehicleCategory; }
  public void setVehicleCategory(String v) { vehicleCategory = v; }
  public Integer getEngineDisplacement() { return engineDisplacement; }
  public void setEngineDisplacement(Integer v) { engineDisplacement = v; }
  public Integer getPower() { return power; }
  public void setPower(Integer v) { power = v; }
  public Integer getMaxSpeed() { return maxSpeed; }
  public void setMaxSpeed(Integer v) { maxSpeed = v; }
  public String getVehicleNotes() { return vehicleNotes; }
  public void setVehicleNotes(String v) { vehicleNotes = v; }
  public Instant getCreatedAt() { return createdAt; }
}
