package com.wedding.cart.controller;

import com.wedding.cart.dto.CartItemDTO;
import com.wedding.cart.dto.CartItemListDTO;
import com.wedding.cart.service.CartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Log4j2
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    // 장바구니 담기/수량변경/삭제(qty<=0)
    @PreAuthorize("#itemDTO.email == authentication.name")
    @PostMapping("/change")
    public List<CartItemListDTO> changeCart(@RequestBody CartItemDTO itemDTO) {

        log.info(itemDTO);

        if (itemDTO.getQty() <= 0) {
            return cartService.remove(itemDTO.getCino(), itemDTO.getEmail());
        }

        return cartService.addOrModify(itemDTO);
    }

    // 장바구니 목록 조회
    @PreAuthorize("hasAnyRole('USER')")
    @GetMapping("/items")
    public List<CartItemListDTO> getCartItems(Principal principal) {

        String email = principal.getName();

        return cartService.getCartItems(email);
    }

    // 장바구니 아이템 삭제
    @PreAuthorize("hasAnyRole('USER')")
    @DeleteMapping("/{cino}")
    public List<CartItemListDTO> removeFromCart(@PathVariable("cino") Long cino, Principal principal) {

        return cartService.remove(cino, principal.getName());
    }
}