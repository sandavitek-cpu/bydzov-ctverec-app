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
@Table(name = "notification")
public class Notification {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(nullable = false)
  private String title;

  @Column(nullable = false, length = 5000)
  private String message;

  @Column(nullable = false, length = 20)
  private String type;

  @Column(name = "related_url", length = 500)
  private String relatedUrl;

  @Column(name = "is_read", nullable = false)
  private Boolean isRead = false;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected Notification() {}

  public Notification(User user, String title, String message, String type, String relatedUrl) {
    this.user = user;
    this.title = title;
    this.message = message;
    this.type = type != null ? type : "INFO";
    this.relatedUrl = relatedUrl;
    this.isRead = false;
    this.createdAt = Instant.now();
  }

  public Long getId() { return id; }
  public User getUser() { return user; }
  public String getTitle() { return title; }
  public String getMessage() { return message; }
  public String getType() { return type; }
  public String getRelatedUrl() { return relatedUrl; }
  public Boolean getIsRead() { return isRead; }
  public Instant getCreatedAt() { return createdAt; }

  public void markRead() { this.isRead = true; }
}
