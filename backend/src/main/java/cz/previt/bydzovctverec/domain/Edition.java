package cz.previt.bydzovctverec.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "edition")
public class Edition {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "edition_year", nullable = false, unique = true)
  private Integer editionYear;

  @Column(nullable = false)
  private String label;

  protected Edition() {}

  public Edition(Integer editionYear, String label) {
    this.editionYear = editionYear;
    this.label = label;
  }

  public Long getId() {
    return id;
  }

  public Integer getEditionYear() {
    return editionYear;
  }

  public String getLabel() {
    return label;
  }
}
