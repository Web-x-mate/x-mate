package xmate.com.controller.web;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import xmate.com.service.cart.CartService;

@Controller
@RequiredArgsConstructor
public class CartPageController {
    private final CartService cartService;

    @GetMapping("/cart")
    public String cart(Model m){ m.addAttribute("cart", cartService.getCartForCurrentUser()); return "cart/index"; }
}
