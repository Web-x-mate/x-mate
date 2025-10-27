package xmate.com.controller.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import xmate.com.controller.client.support.ClientCatalogViewService;
import xmate.com.controller.client.view.PaginationView;
import xmate.com.controller.client.view.ProductCardView;
import xmate.com.entity.catalog.Product;
import xmate.com.service.catalog.ProductService;

import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ClientHomeController {

    private final ProductService productService;
    private final ClientCatalogViewService catalogViewService;
    private static final int DEFAULT_PAGE_SIZE = 12;
    private static final int MAX_PAGE_SIZE = 48;
    private static final Sort HOME_SORT = Sort.by("createdAt").descending();


    @GetMapping({"/", "/home"})
    public String home(@RequestParam(name = "q", required = false) String query,
                       @RequestParam(name = "page", defaultValue = "1") int pageParam,
                       @RequestParam(name = "size", defaultValue = "12") int sizeParam,
                       Model model) {

        String sanitizedQuery = query != null ? query.trim() : null;
        boolean isSearch = sanitizedQuery != null && !sanitizedQuery.isBlank();
        int sanitizedPage = Math.max(pageParam, 1);
        int sanitizedSize = sizeParam <= 0 ? DEFAULT_PAGE_SIZE : Math.min(sizeParam, MAX_PAGE_SIZE);

        Page<Product> page = loadProducts(isSearch, sanitizedQuery,
                PageRequest.of(sanitizedPage - 1, sanitizedSize, HOME_SORT));

        if (sanitizedPage > 1 && page.getTotalPages() > 0 && sanitizedPage > page.getTotalPages()) {
            page = loadProducts(isSearch, sanitizedQuery,
                    PageRequest.of(page.getTotalPages() - 1, sanitizedSize, HOME_SORT));
        }

        List<ProductCardView> cards = catalogViewService.toProductCards(page.getContent());
        PaginationView pagination = catalogViewService.buildPagination(page);

        log.info("[HOME] query='{}', isSearch={}, page={}/{} size={} fetched {} products",
                sanitizedQuery,
                isSearch,
                page.getNumber() + 1,
                Math.max(page.getTotalPages(), 1),
                page.getSize(),
                page.getNumberOfElements());

        if (cards.isEmpty()) log.warn("[HOME] No products available to render.");
        else log.info("[HOME] First card: {}", cards.get(0));

        model.addAttribute("pageTitle", "X-Mate | Trang chu");
        model.addAttribute("isSearch", isSearch);
        model.addAttribute("searchQuery", sanitizedQuery);
        model.addAttribute("searchTotal", page.getTotalElements());
        model.addAttribute("products", cards);
        model.addAttribute("pagination", pagination);
        model.addAttribute("primaryCategories", catalogViewService.buildPrimaryNav());
        model.addAttribute("cartQuantity", 0);

        return "client/home/index";
    }

    private Page<Product> loadProducts(boolean isSearch, String query, PageRequest pageRequest) {
        return isSearch ? productService.search(query, pageRequest) : productService.list(pageRequest);
    }

}
