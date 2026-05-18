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
@Table(name = "schedule_item")
public class ScheduleItem {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "edition_id", nullable = false)
  private Edition edition;

  @Column(nullable = false, length = 10)
  private String time;

  @Column(nullable = false, length = 200)
  private String label;

  @Column(length = 500)
  private String description;

  @Column(name = "sort_order")
  private Integer sortOrder;

  protected ScheduleItem() {}

  public ScheduleItem(Edition edition, String time, String label, String description, Integer sortOrder) {
    this.edition = edition;
    this.time = time;
    this.label = label;
    this.description = description;
    this.sortOrder = sortOrder;
  }

  public Long getId() { return id; }
  public Edition getEdition() { return edition; }
  public String getTime() { return time; }
  public void setTime(String time) { this.time = time; }
  public String getLabel() { return label; }
  public void setLabel(String label) { this.label = label; }
  public String getDescription() { return description; }
  public void setDescription(String description) { this.description = description; }
  public Integer getSortOrder() { return sortOrder; }
  public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
