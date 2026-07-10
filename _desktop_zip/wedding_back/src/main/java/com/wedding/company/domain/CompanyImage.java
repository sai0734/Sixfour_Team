package com.wedding.company.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CompanyImage {

  @Column(name = "file_name", nullable = false)
  private String fileName;

  @Column(name = "ord")
  private int ord;

  public void changeOrd(int ord) {
    this.ord = ord;
  }
}
