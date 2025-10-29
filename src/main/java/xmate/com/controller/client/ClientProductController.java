package xmate.com.controller.client;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import xmate.com.controller.client.support.ClientCatalogViewService;
import xmate.com.controller.client.view.ProductDetailView;
import xmate.com.entity.catalog.Product;
import xmate.com.service.catalog.ProductService;

import java.util.Optional;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ClientProductController {

    private final ProductService productService;
    private final ClientCatalogViewService catalogViewService;

    @GetMapping("/product/detail/{slug}")
    public String detail(@PathVariable("slug") String slug,
                         HttpServletRequest request,
                         Model model) {

        String sanitizedSlug = slug == null ? "" : slug.trim();
        model.addAttribute("primaryCategories", catalogViewService.buildPrimaryNav());
        model.addAttribute("cartQuantity", 0);
        model.addAttribute("searchQuery", null);

        if (sanitizedSlug.isEmpty()) {
            log.warn("[PRODUCT DETAIL] Empty slug provided");
            model.addAttribute("product", null);
            model.addAttribute("pageTitle", "San pham");
            return "client/product/detail";
        }

        Optional<Product> productOpt = productService.findBySlug(sanitizedSlug);
        if (productOpt.isEmpty()) {
            log.warn("[PRODUCT DETAIL] Not found slug={}", sanitizedSlug);
            model.addAttribute("product", null);
            model.addAttribute("pageTitle", "San pham");
            return "client/product/detail";
        }

        String shareUrl = ServletUriComponentsBuilder.fromRequest(request)
                .build()
                .toUriString();

        ProductDetailView detailView = catalogViewService.toProductDetail(productOpt.get(), shareUrl);

        model.addAttribute("product", detailView);
        model.addAttribute("pageTitle", detailView.title() != null ? detailView.title() + " | X-Mate" : "San pham");

        return "client/product/detail";
    }
}
