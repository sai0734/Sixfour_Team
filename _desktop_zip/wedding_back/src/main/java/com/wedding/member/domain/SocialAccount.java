package com.wedding.member.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString(exclude = "member")
@Table(name = "tbl_social_account")
public class SocialAccount {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "email", nullable = false)
  private Member member;

  @Builder.Default
  @Column(nullable = false)
  private String provider = "kakao";

  @Column(unique = true, nullable = false)
  private String providerId;

}