package com.wedding.cart.service;

import com.wedding.cart.dto.CartItemDTO;
import com.wedding.cart.dto.CartItemListDTO;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional
public interface CartService {

    // 장바구니 담기/수량 변경
    List<CartItemListDTO> addOrModify(CartItemDTO cartItemDTO);

    // 장바구니 목록 조회
    List<CartItemListDTO> getCartItems(String email);

    // 장바구니 아이템 삭제
    List<CartItemListDTO> remove(Long cino);
}