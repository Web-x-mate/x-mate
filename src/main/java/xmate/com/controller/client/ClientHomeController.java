package xmate.com.controller.client;

import jakarta.servlet.http.HttpServletRequest;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Function;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ClientHomeController {

    private final ProductService productService;
    private final ClientCatalogViewService catalogViewService;
    private static final int DEFAULT_PAGE_SIZE = 12;
    private static final int MAX_PAGE_SIZE = 48;
    private static final Sort HOME_SORT = Sort.by("createdAt").descending();
    private static final int NEW_ARRIVAL_DAYS = 7;


    @GetMapping({"/", "/home"})
    public String home(@RequestParam(name = "q", required = false) String query,
                       @RequestParam(name = "page", defaultValue = "1") int pageParam,
                       @RequestParam(name = "size", defaultValue = "12") int sizeParam,
                       HttpServletRequest request,
                       Model model) {

        String sanitizedQuery = query != null ? query.trim() : null;
        boolean isSearch = sanitizedQuery != null && !sanitizedQuery.isBlank();
        int sanitizedPage = sanitizePage(pageParam);
        int sanitizedSize = sanitizeSize(sizeParam);

        Page<Product> page = fetchPage(sanitizedPage, sanitizedSize,
                pr -> loadProducts(isSearch, sanitizedQuery, pr));

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

        String uri = request.getRequestURI();
        String ctx = request.getContextPath();
        boolean isRoot = uri.equals(ctx + "/");
        boolean isHomeAlias = uri.equals(ctx + "/home");
        boolean hasQuery = request.getQueryString() != null && !request.getQueryString().isBlank();
        boolean showHomeBanner = (isRoot || isHomeAlias) && !isSearch && !hasQuery;

        return renderListing(model, "X-Mate | Trang chủ", null, isSearch, sanitizedQuery, page, cards, pagination, showHomeBanner);
    }

    @GetMapping("/hang-moi")
    public String newArrivals(@RequestParam(name = "page", defaultValue = "1") int pageParam,
                              @RequestParam(name = "size", defaultValue = "12") int sizeParam,
                              Model model) {
        int sanitizedPage = sanitizePage(pageParam);
        int sanitizedSize = sanitizeSize(sizeParam);
        LocalDateTime since = LocalDateTime.now().minusDays(NEW_ARRIVAL_DAYS);

        Page<Product> page = fetchPage(sanitizedPage, sanitizedSize,
                pr -> productService.listNewArrivals(since, pr));
        List<ProductCardView> cards = catalogViewService.toProductCards(page.getContent());
        PaginationView pagination = catalogViewService.buildPagination(page);

        log.info("[HOME] New arrivals since {} page={}/{} size={} fetched {} products",
                since, page.getNumber() + 1, Math.max(page.getTotalPages(), 1), page.getSize(), page.getNumberOfElements());

        return renderListing(model,
                "X-Mate | Hàng mới",
                "Hàng mới về 7 ngày qua",
                false,
                null,
                page,
                cards,
                pagination,
                false);
    }

    @GetMapping("/the-thao")
    public String sports(@RequestParam(name = "page", defaultValue = "1") int pageParam,
                         @RequestParam(name = "size", defaultValue = "12") int sizeParam,
                         Model model) {
        int sanitizedPage = sanitizePage(pageParam);
        int sanitizedSize = sanitizeSize(sizeParam);

        Page<Product> page = fetchPage(sanitizedPage, sanitizedSize,
                pr -> productService.findBySlugKeyword("the-thao", pr));
        List<ProductCardView> cards = catalogViewService.toProductCards(page.getContent());
        PaginationView pagination = catalogViewService.buildPagination(page);

        log.info("[HOME] Sports collection page={}/{} size={} fetched {} products",
                page.getNumber() + 1, Math.max(page.getTotalPages(), 1), page.getSize(), page.getNumberOfElements());

        return renderListing(model,
                "X-Mate | Thể thao",
                "Bộ sưu tập thể thao",
                false,
                null,
                page,
                cards,
                pagination,
                false);
    }

    @GetMapping("/sale")
    public String sale(@RequestParam(name = "page", defaultValue = "1") int pageParam,
                       @RequestParam(name = "size", defaultValue = "12") int sizeParam,
                       Model model) {
        int sanitizedPage = sanitizePage(pageParam);
        int sanitizedSize = sanitizeSize(sizeParam);

        Page<Product> page = fetchPage(sanitizedPage, sanitizedSize,
                pr -> productService.listDiscounted(0.5, pr));
        List<ProductCardView> cards = catalogViewService.toProductCards(page.getContent());
        PaginationView pagination = catalogViewService.buildPagination(page);

        log.info("[HOME] Sale 50%% page={}/{} size={} fetched {} products",
                page.getNumber() + 1, Math.max(page.getTotalPages(), 1), page.getSize(), page.getNumberOfElements());

        return renderListing(model,
                "X-Mate | Sale 50%",
                "Ưu đãi từ 50%",
                false,
                null,
                page,
                cards,
                pagination,
                false);
    }

    private Page<Product> loadProducts(boolean isSearch, String query, PageRequest pageRequest) {
        return isSearch ? productService.search(query, pageRequest) : productService.list(pageRequest);
    }

    private Page<Product> fetchPage(int sanitizedPage,
                                    int sanitizedSize,
                                    Function<PageRequest, Page<Product>> loader) {
        PageRequest request = PageRequest.of(sanitizedPage - 1, sanitizedSize, HOME_SORT);
        Page<Product> page = loader.apply(request);
        if (sanitizedPage > 1 && page.getTotalPages() > 0 && sanitizedPage > page.getTotalPages()) {
            PageRequest lastRequest = PageRequest.of(page.getTotalPages() - 1, sanitizedSize, HOME_SORT);
            page = loader.apply(lastRequest);
        }
        return page;
    }

    private String renderListing(Model model,
                                 String pageTitle,
                                 String sectionTitle,
                                 boolean isSearch,
                                 String searchQuery,
                                 Page<Product> page,
                                 List<ProductCardView> cards,
                                 PaginationView pagination,
                                 boolean showHomeBanner) {
        model.addAttribute("pageTitle", pageTitle);
        model.addAttribute("sectionTitle", sectionTitle);
        model.addAttribute("isSearch", isSearch);
        model.addAttribute("searchQuery", searchQuery);
        model.addAttribute("searchTotal", page.getTotalElements());
        model.addAttribute("products", cards);
        model.addAttribute("pagination", pagination);
        model.addAttribute("primaryCategories", catalogViewService.buildPrimaryNav());
        model.addAttribute("cartQuantity", 0);
        model.addAttribute("showHomeBanner", showHomeBanner);
        return "client/home/index";
    }

    private int sanitizePage(int pageParam) {
        return Math.max(pageParam, 1);
    }

    private int sanitizeSize(int sizeParam) {
        if (sizeParam <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(sizeParam, MAX_PAGE_SIZE);
    }

}
