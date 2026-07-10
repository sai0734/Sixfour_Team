package com.wedding.cart.repository;

import com.wedding.cart.domain.CartItem;
import com.wedding.cart.dto.CartItemListDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    // 회원 이메일로 장바구니 아이템 목록 조회 (옵션 정보 포함)
    @Query("select new com.wedding.cart.dto.CartItemListDTO(" +
            "ci.cino, ci.qty, p.pno, p.pname, p.price, pi.fileName, " +
            "po.pono, po.optionName, po.optionValue, po.extraPrice) " +
            "from CartItem ci " +
            "inner join ci.cartOrder co " +
            "inner join ci.product p " +
            "left join p.imageList pi " +
            "left join ci.productOption po " +
            "where co.member.email = :email and (pi.ord = 0 or pi is null) " +
            "order by ci.cino desc")
    List<CartItemListDTO> getItemsOfCartDTOByEmail(@Param("email") String email);

    // 같은 상품 + 같은 옵션(또는 옵션 없음 상태)이 이미 담겨있는지 확인
    @Query("select ci from CartItem ci inner join ci.cartOrder co " +
            "where co.member.email = :email and ci.product.pno = :pno " +
            "and ((:pono is null and ci.productOption is null) or ci.productOption.pono = :pono)")
    CartItem getItemOfPnoAndOption(@Param("email") String email, @Param("pno") Long pno, @Param("pono") Long pono);

    // 장바구니 아이템 번호로 소속 장바구니 번호 조회
    @Query("select co.cno from CartItem ci inner join ci.cartOrder co where ci.cino = :cino")
    Long getCartFromItem(@Param("cino") Long cino);

    // 장바구니 번호로 아이템 목록 조회 (옵션 정보 포함)
    @Query("select new com.wedding.cart.dto.CartItemListDTO(" +
            "ci.cino, ci.qty, p.pno, p.pname, p.price, pi.fileName, " +
            "po.pono, po.optionName, po.optionValue, po.extraPrice) " +
            "from CartItem ci " +
            "inner join ci.cartOrder co " +
            "inner join ci.product p " +
            "left join p.imageList pi " +
            "left join ci.productOption po " +
            "where co.cno = :cno and (pi.ord = 0 or pi is null) " +
            "order by ci.cino desc")
    List<CartItemListDTO> getItemsOfCartDTOByCart(@Param("cno") Long cno);

}