package com.wedding.product.domain;

import com.wedding.global.advice.NotEnoughStockException;
import com.wedding.global.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tbl_product")
@Getter
@ToString(exclude = "imageList")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Product extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long pno;

  @Column(nullable = false)
  private String pname;

  @Column(nullable = false)
  private int price;

  private String pdesc;

  @Column(columnDefinition = "boolean default false")
  private boolean delFlag;

  @Column(nullable = true)
  private String category;

  @Column(columnDefinition = "int default 0")
  private int stockQty;

  @Column(columnDefinition = "double default 0.0")
  private double ratingAvg;

  @Column(columnDefinition = "int default 0")
  private int reviewCount;

  @ElementCollection
  @CollectionTable(name = "tbl_product_image", joinColumns = @JoinColumn(name = "product_pno"))
  @Builder.Default
  private List<ProductImage> imageList = new ArrayList<>();

  @Column(columnDefinition = "bigint default 0")
  private Long viewCount;

  @Column(columnDefinition = "int default 0")
  private int salesCount;


  public void changeName(String name){
    this.pname = name;
  }

  public void changePrice(int price) {
    this.price = price;
  }

  public void changeDesc(String desc){
      this.pdesc = desc;
  }

  public void changeCategory(String category){
    this.category = category;
  }

  public void changeStockQty(int stockQty){
    this.stockQty = stockQty;
  }

  public void changeDel(boolean delFlag) {
    this.delFlag = delFlag;
  }

  public void addImage(ProductImage image) {

      image.setOrd(this.imageList.size());
      imageList.add(image);

  }

  public void addImageString(String fileName){

    ProductImage productImage = ProductImage.builder()
    .fileName(fileName)
    .build();
    addImage(productImage);

  }

  public void clearList() {
    this.imageList.clear();
  }

  public void reduceStock(int qty) {

    if(this.stockQty < qty) {
      throw new NotEnoughStockException("재고가 부족합니다.");
    }

    this.stockQty -= qty;

  }

}