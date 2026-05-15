package cz.previt.bydzovctverec.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "app_user")
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String email;

  @Column(nullable = false, unique = true)
  private String username;

  @Column(nullable = false)
  private String password;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private UserRole role;

  @Column(name = "first_name", nullable = false)
  private String firstName;

  @Column(name = "last_name", nullable = false)
  private String lastName;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(length = 30)
  private String phone;

  @Column(name = "member_since")
  private LocalDate memberSince;

  @ManyToMany(fetch = FetchType.EAGER)
  @JoinTable(
      name = "user_app_role",
      joinColumns = @JoinColumn(name = "user_id"),
      inverseJoinColumns = @JoinColumn(name = "app_role_id"))
  private Set<AppRole> appRoles = new HashSet<>();

  protected User() {}

  public User(Long id, String email, String username, String password, UserRole role, String firstName, String lastName, Instant createdAt) {
    this.id = id;
    this.email = email;
    this.username = username;
    this.password = password;
    this.role = role;
    this.firstName = firstName;
    this.lastName = lastName;
    this.createdAt = createdAt;
  }

  public User(String email, String username, String password, UserRole role, String firstName, String lastName, Instant createdAt) {
    this.email = email;
    this.username = username;
    this.password = password;
    this.role = role;
    this.firstName = firstName;
    this.lastName = lastName;
    this.createdAt = createdAt;
  }

  public Long getId() { return id; }
  public String getEmail() { return email; }
  public void setEmail(String email) { this.email = email; }
  public String getUsername() { return username; }
  public String getPassword() { return password; }
  public void setPassword(String password) { this.password = password; }
  public UserRole getRole() { return role; }
  public void setRole(UserRole role) { this.role = role; }
  public String getFirstName() { return firstName; }
  public void setFirstName(String firstName) { this.firstName = firstName; }
  public String getLastName() { return lastName; }
  public void setLastName(String lastName) { this.lastName = lastName; }
  public String getName() { return lastName.isEmpty() ? firstName : firstName + " " + lastName; }
  public void setName(String name) { var parts = name.split(" ", 2); this.firstName = parts[0]; this.lastName = parts.length > 1 ? parts[1] : ""; }
  public Instant getCreatedAt() { return createdAt; }
  public Set<AppRole> getAppRoles() { return appRoles; }
  public String getPhone() { return phone; }
  public void setPhone(String phone) { this.phone = phone; }
  public LocalDate getMemberSince() { return memberSince; }
  public void setMemberSince(LocalDate memberSince) { this.memberSince = memberSince; }
}
