package cz.previt.bydzovctverec.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "crew_member")
public class CrewMember {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "racer_registration_id", nullable = false)
  private RacerRegistration registration;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  private User user;

  @Column(name = "first_name", nullable = false, length = 100)
  private String firstName;

  @Column(name = "last_name", nullable = false, length = 100)
  private String lastName;

  @Column(nullable = false)
  private String email;

  @Column(name = "driver_age")
  private Integer driverAge;

  @Column(length = 10)
  private String gender;

  @Column(length = 300)
  private String address;

  @Column(name = "club_member")
  private Boolean clubMember;

  @Column(name = "first_time")
  private Boolean firstTime;

  protected CrewMember() {}

  public CrewMember(RacerRegistration registration, User user, String firstName, String lastName, String email) {
    this.registration = registration;
    this.user = user;
    this.firstName = firstName;
    this.lastName = lastName;
    this.email = email;
  }

  public CrewMember(RacerRegistration registration, User user, String firstName, String lastName,
      String email, Integer driverAge, String gender, String address, Boolean clubMember, Boolean firstTime) {
    this(registration, user, firstName, lastName, email);
    this.driverAge = driverAge;
    this.gender = gender;
    this.address = address;
    this.clubMember = clubMember;
    this.firstTime = firstTime;
  }

  public Long getId() { return id; }
  public RacerRegistration getRegistration() { return registration; }
  public User getUser() { return user; }
  public String getFirstName() { return firstName; }
  public String getLastName() { return lastName; }
  public String getEmail() { return email; }
  public Integer getDriverAge() { return driverAge; }
  public String getGender() { return gender; }
  public String getAddress() { return address; }
  public Boolean getClubMember() { return clubMember; }
  public Boolean getFirstTime() { return firstTime; }
}
