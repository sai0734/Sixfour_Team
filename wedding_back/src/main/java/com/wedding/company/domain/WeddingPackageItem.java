package com.wedding.company.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "tbl_wedding_package_item")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString(exclude = "weddingPackage")

public class WeddingPackageItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long itemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wedding_package_id", nullable = false)
    private WeddingPackage weddingPackage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CompanyCategory category;

    private Long cmno;

    private String companyName;

    private String optionLabel;

    private BigDecimal price;

    private Integer sortOrder;

    void bindPackage(WeddingPackage weddingPackage) {
        this.weddingPackage = weddingPackage;
    }
}
