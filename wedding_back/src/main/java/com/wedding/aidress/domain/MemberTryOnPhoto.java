package com.wedding.aidress.domain;

import com.wedding.global.domain.BaseTimeEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "tbl_member_try_on_photo")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class MemberTryOnPhoto extends BaseTimeEntity {

  @Id
  private String memberEmail;

  private String photoFileName;

  public void changePhoto(String fileName) {
    this.photoFileName = fileName;
  }
}
