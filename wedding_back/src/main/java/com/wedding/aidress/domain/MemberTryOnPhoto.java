package com.wedding.aidress.domain;

import com.wedding.global.domain.BaseTimeEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tbl_member_tryon_photo")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor

public class MemberTryOnPhoto extends BaseTimeEntity {

    @Id
    private String memberEmail;

    private String photoFileName;

    public void changePhoto(String photoFileName) {
        this.photoFileName = photoFileName;
    }
}
