package cz.previt.bydzovctverec.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "message_log")
public class MessageLog {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "recipient_type", nullable = false, length = 50)
  private String recipientType;

  @Column(nullable = false, length = 500)
  private String subject;

  @Column(nullable = false, length = 5000)
  private String body;

  @Column(name = "recipient_count", nullable = false)
  private Integer recipientCount;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected MessageLog() {}

  public MessageLog(String recipientType, String subject, String body, Integer recipientCount, Instant createdAt) {
    this.recipientType = recipientType;
    this.subject = subject;
    this.body = body;
    this.recipientCount = recipientCount;
    this.createdAt = createdAt;
  }

  public Long getId() { return id; }
  public String getRecipientType() { return recipientType; }
  public String getSubject() { return subject; }
  public String getBody() { return body; }
  public Integer getRecipientCount() { return recipientCount; }
  public Instant getCreatedAt() { return createdAt; }
}
