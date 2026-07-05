package com.wedding.cart.service;

import com.wedding.cart.domain.CartItem;
import com.wedding.cart.domain.CartOrder;
import com.wedding.cart.dto.CartItemDTO;
import com.wedding.cart.dto.CartItemListDTO;
import com.wedding.cart.repository.CartItemRepository;
import com.wedding.cart.repository.CartRepository;
import com.wedding.member.domain.Member;
import com.wedding.product.domain.Product;
import com.wedding.product.domain.ProductOption;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@RequiredArgsConstructor
@Service
@Log4j2
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;

    // 장바구니 담기/수량 변경
    @Override
    public List<CartItemListDTO> addOrModify(CartItemDTO cartItemDTO) {

        String email = cartItemDTO.getEmail();
        Long pno = cartItemDTO.getPno();
        Long pono = cartItemDTO.getPono();
        int qty = cartItemDTO.getQty();
        Long cino = cartItemDTO.getCino();

        if (cino != null) { // 장바구니 아이템 번호가 있으면 수량만 변경
            CartItem cartItem = cartItemRepository.findById(cino)
                    .orElseThrow(() -> new NoSuchElementException("장바구니 아이템이 없습니다. cino=" + cino));

            cartItem.changeQty(qty);
            cartItemRepository.save(cartItem);

            return getCartItems(email);
        }

        // cino가 없는 경우: 신규 담기 혹은 이미 담긴 동일 상품+옵션의 수량 변경
        CartOrder cart = getCart(email);

        CartItem cartItem = cartItemRepository.getItemOfPnoAndOption(email, pno, pono);

        if (cartItem == null) {
            Product product = Product.builder().pno(pno).build();

            CartItem.CartItemBuilder builder = CartItem.builder()
                    .product(product)
                    .cartOrder(cart)
                    .qty(qty);

            if (pono != null) {
                ProductOption productOption = ProductOption.builder().pono(pono).build();
                builder.productOption(productOption);
            }

            cartItem = builder.build();
        } else {
            cartItem.changeQty(qty);
        }

        cartItemRepository.save(cartItem);

        return getCartItems(email);
    }

    // 회원의 장바구니가 없으면 새로 생성
    private CartOrder getCart(String email) {

        Optional<CartOrder> result = cartRepository.getCartOfMember(email);

        if (result.isEmpty()) {
            log.info("장바구니가 없어 새로 생성합니다. email=" + email);

            Member member = Member.builder().email(email).build();
            CartOrder tempCart = CartOrder.builder().member(member).build();

            return cartRepository.save(tempCart);
        }

        return result.get();
    }

    // 장바구니 목록 조회
    @Override
    public List<CartItemListDTO> getCartItems(String email) {
        return cartItemRepository.getItemsOfCartDTOByEmail(email);
    }

    // 장바구니 아이템 삭제
    @Override
    public List<CartItemListDTO> remove(Long cino) {

        Long cno = cartItemRepository.getCartFromItem(cino);

        cartItemRepository.deleteById(cino);

        return cartItemRepository.getItemsOfCartDTOByCart(cno);
    }
}