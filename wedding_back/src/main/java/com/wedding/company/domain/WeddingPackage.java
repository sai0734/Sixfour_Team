package com.wedding.company.domain;

import com.wedding.global.domain.BaseTimeEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name="tbl_wedding_package")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString(exclude = "items")

public class WeddingPackage extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long weddingPackageId;

    @Column(nullable = false)
    private String name;

    @Column(length = 2000)
    private String description;

    private BigDecimal totalPrice;

    private BigDecimal salePrice;

    private String thumbnail;

    @Column(columnDefinition = "boolean default false")
    private boolean delFlag;

    @OneToMany(mappedBy = "weddingPackage", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<WeddingPackageItem> items = new ArrayList<>();

    public void change(String name, String description, BigDecimal totalPrice,
                       BigDecimal salePrice, String thumbnail) {
        this.name = name;
        this.description = description;
        this.totalPrice = totalPrice;
        this.salePrice = salePrice;
        this.thumbnail = thumbnail;
    }

    public void addItem(WeddingPackageItem item) {
        this.items.add(item);
        item.bindPackage(this);
    }

    public void clearItems() {
        this.items.clear();
    }

    public void remove() {
        this.delFlag = true;
    }
}

