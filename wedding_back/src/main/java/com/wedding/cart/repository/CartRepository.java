//package com.wedding.cart.repository;
//
//import java.util.Optional;
//
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.Query;
//import org.springframework.data.repository.query.Param;
//
//import com.wedding.cart.domain.CartOrder;
//
//public interface CartRepository extends JpaRepository<CartOrder, Long>{
//
//    @Query("select cart from Cart cart where cart.owner.email = :email")
//    public Optional<CartOrder> getCartOfMember(@Param("email") String email);
//
//}