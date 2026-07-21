package com.wedding.aidress.domain;

import com.wedding.global.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "tbl_member_try_on_history")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class MemberTryOnHistory extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long historyId;

  @Column(nullable = false, length = 100)
  private String memberEmail;

  private Long dressItemId;

  @Column(length = 200)
  private String dressName;

  /** CatVTON 합성 결과 파일명 */
  @Column(nullable = false, length = 255)
  private String tryOnFileName;

  /** 최종 표시 이미지(배경 적용 후). 배경 미적용 시 tryOnFileName과 동일 */
  @Column(nullable = false, length = 255)
  private String resultFileName;

  @Column(length = 1000)
  private String backgroundPrompt;

  public void changeBackground(String prompt, String newResultFileName) {
    this.backgroundPrompt = prompt;
    this.resultFileName = newResultFileName;
  }
}
