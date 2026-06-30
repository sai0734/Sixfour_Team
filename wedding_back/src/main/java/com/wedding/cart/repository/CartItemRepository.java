package com.wedding.cart.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.wedding.cart.domain.CartItem;
import com.wedding.cart.dto.CartItemListDTO;

public interface CartItemRepository extends JpaRepository<CartItem, Long>{

    // 사용자의 이메일을 기반으로 해당 사용자의 장바구니에 담긴 모든 아이템 목록을 조회
  @Query("select " + 
  " new com.wedding.cart.dto.CartItemListDTO(ci.cino,  ci.qty,  p.pno, p.pname, p.price , pi.fileName )  " +
  " from " +
  "   CartItem ci inner join Cart mc on ci.cart = mc " +
  "   left join Product p on ci.product = p " +
  "   left join p.imageList pi" +
  " where " +
  "   mc.owner.email = :email and pi.ord = 0 " +
  " order by ci desc ")
  public List<CartItemListDTO> getItemsOfCartDTOByEmail(@Param("email") String email);


  // 사용자의 장바구니에 특정상품(pno) 이 이미 담겨있는지 확인하거나 해당 아이템을 가져올 때 
  @Query("select" +
  " ci "+
  " from " + 
  "   CartItem ci inner join Cart c on ci.cart = c " + 
  " where " +
  "   c.owner.email = :email and ci.product.pno = :pno")
  public CartItem getItemOfPno(@Param("email") String email, @Param("pno") Long pno );


  // 장바구니 상품의 식별번호(cino)를 통해 해당 아이템이 속해 있는 장바구니의 번호(cno)를 조회
  @Query("select " + 
  "  c.cno " +
  "from " +
  "  Cart c inner join CartItem ci on ci.cart = c " +
  " where " +
  "  ci.cino = :cino")
  public Long getCartFromItem( @Param("cino") Long cino);
  

  // 사용자의 이메일 대신 장바구니 번호(cno) 를 직접 기반으로 하여 해당 장바구니에 담긴 모든 아이템 목록 조회
  // 장바구니 고유번호(cno) 를 조건으로 조회를 한다는 것은 장바구니 id를 이미 가지고 있는 상태에서 
  // 내부 아이템 목록을 리프레시하거나 조회 할 때 효율적이다.
    @Query("select new com.wedding.cart.dto.CartItemListDTO(ci.cino,  ci.qty,  p.pno, p.pname, p.price , pi.fileName )  " +
  " from " + 
  "   CartItem ci inner join Cart mc on ci.cart = mc " +
  "   left join Product p on ci.product = p " +
  "   left join p.imageList pi" +
  " where " + 
  "  mc.cno = :cno and pi.ord = 0 " + 
  " order by ci desc ")
  public List<CartItemListDTO> getItemsOfCartDTOByCart(@Param("cno") Long cno);

}