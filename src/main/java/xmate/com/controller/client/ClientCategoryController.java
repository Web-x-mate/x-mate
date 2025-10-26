package xmate.com.controller.client;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import xmate.com.controller.client.support.ClientCatalogViewService;
import xmate.com.controller.client.view.CategoryDetailView;
import xmate.com.controller.client.view.CategoryTileView;
import xmate.com.controller.client.view.PaginationView;
import xmate.com.controller.client.view.ProductCardView;
import xmate.com.controller.client.view.ProductColorView;
import xmate.com.entity.catalog.Category;
import xmate.com.entity.catalog.Product;
import xmate.com.service.catalog.CategoryService;
import xmate.com.service.catalog.ProductService;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ClientCategoryController {

    private static final int DEFAULT_PAGE_SIZE = 12;
    private static final int MAX_PAGE_SIZE = 48;
    private static final Sort CATEGORY_SORT = Sort.by("createdAt").descending();
    private static final String MALE_SLUG = "nam";
    private static final String FEMALE_SLUG = "nu";

    private final CategoryService categoryService;
    private final ProductService productService;
    private final ClientCatalogViewService catalogViewService;

    @GetMapping("/category/{slug}")
    public String detail(@PathVariable("slug") String slug,
                         @RequestParam(name = "page", defaultValue = "1") int pageParam,
                         @RequestParam(name = "size", defaultValue = "12") int sizeParam,
                         @RequestParam(name = "sizeFilter", required = false) List<String> sizeFilters,
                         @RequestParam(name = "colorFilter", required = false) List<String> colorFilters,
                         @RequestParam(name = "priceRange", required = false) List<String> priceRanges,
                         Model model) {
        return renderCategoryPage(slug, pageParam, sizeParam, sizeFilters, colorFilters, priceRanges, model);
    }

    @GetMapping({"/nam", "/nu"})
    public String genderCollections(HttpServletRequest request,
                                    @RequestParam(name = "page", defaultValue = "1") int pageParam,
                                    @RequestParam(name = "size", defaultValue = "12") int sizeParam,
                                    @RequestParam(name = "sizeFilter", required = false) List<String> sizeFilters,
                                    @RequestParam(name = "colorFilter", required = false) List<String> colorFilters,
                                    @RequestParam(name = "priceRange", required = false) List<String> priceRanges,
                                    Model model) {
        String uri = request.getRequestURI();
        String slug = uri != null && uri.toLowerCase().contains("/nu") ? FEMALE_SLUG : MALE_SLUG;
        return renderCategoryPage(slug, pageParam, sizeParam, sizeFilters, colorFilters, priceRanges, model);
    }

    private String renderCategoryPage(String slug,
                                      int pageParam,
                                      int sizeParam,
                                      List<String> sizeFilters,
                                      List<String> colorFilters,
                                      List<String> priceRanges,
                                      Model model) {
        String sanitizedSlug = slug == null ? "" : slug.trim();
        int sanitizedPage = Math.max(pageParam, 1);
        int sanitizedSize = sizeParam <= 0 ? DEFAULT_PAGE_SIZE : Math.min(sizeParam, MAX_PAGE_SIZE);

        model.addAttribute("primaryCategories", catalogViewService.buildPrimaryNav());
        model.addAttribute("cartQuantity", 0);
        model.addAttribute("searchQuery", null);

        if (sanitizedSlug.isEmpty()) {
            log.warn("[CATEGORY] Empty slug");
            populateCategoryNotFound(model);
            return "client/category/index";
        }

        Optional<Category> categoryOpt = categoryService.findBySlug(sanitizedSlug);
        if (categoryOpt.isEmpty()) {
            log.warn("[CATEGORY] Not found slug={}", sanitizedSlug);
            populateCategoryNotFound(model);
            return "client/category/index";
        }

        Category parent = categoryOpt.get();
        CategoryDetailView parentView = catalogViewService.toCategoryDetail(parent);
        List<Category> children = categoryService.findChildren(parent.getId());
        List<CategoryTileView> childTiles = catalogViewService.toCategoryTiles(children);
        List<CategoryDetailView> breadcrumb = buildBreadcrumb(parent);

        Set<Long> categoryIds = new LinkedHashSet<>();
        categoryIds.add(parent.getId());
        categoryIds.addAll(collectDescendantIds(parent.getId()));

        List<String> normalizedSizes = normalizeFilterValues(sizeFilters);
        List<String> normalizedColors = normalizeFilterValues(colorFilters);
        List<PriceRange> parsedPriceRanges = parsePriceRanges(priceRanges);
        boolean hasFilters = !normalizedSizes.isEmpty() || !normalizedColors.isEmpty() || !parsedPriceRanges.isEmpty();

        List<ProductCardView> cards;
        PaginationView pagination;
        long totalElements;

        if (hasFilters) {
            List<Product> allProducts = productService.listByCategories(categoryIds);
            List<ProductCardView> allCards = catalogViewService.toProductCards(allProducts);
            List<ProductCardView> filteredCards = allCards.stream()
                    .filter(card -> matchesSize(card, normalizedSizes))
                    .filter(card -> matchesColor(card, normalizedColors))
                    .filter(card -> matchesPrice(card, parsedPriceRanges))
                    .collect(Collectors.toList());

            totalElements = filteredCards.size();
            int fromIndex = Math.min(Math.max((sanitizedPage - 1) * sanitizedSize, 0), (int) totalElements);
            int toIndex = Math.min(fromIndex + sanitizedSize, (int) totalElements);
            cards = filteredCards.subList(fromIndex, toIndex);
            Page<ProductCardView> stubPage = new PageImpl<>(cards, PageRequest.of(sanitizedPage - 1, sanitizedSize), totalElements);
            pagination = catalogViewService.buildPagination(stubPage);
        } else {
            PageRequest pageRequest = PageRequest.of(sanitizedPage - 1, sanitizedSize, CATEGORY_SORT);
            Page<Product> productPage = productService.byCategories(categoryIds, pageRequest);
            if (sanitizedPage > 1 && productPage.getTotalPages() > 0 && sanitizedPage > productPage.getTotalPages()) {
                int lastPageIdx = Math.max(productPage.getTotalPages() - 1, 0);
                productPage = productService.byCategories(
                        categoryIds,
                        PageRequest.of(lastPageIdx, sanitizedSize, CATEGORY_SORT)
                );
            }
            cards = catalogViewService.toProductCards(productPage.getContent());
            pagination = catalogViewService.buildPagination(productPage);
            totalElements = productPage.getTotalElements();
        }

        model.addAttribute("parent", parentView);
        model.addAttribute("category", parentView);
        String resolvedSlug = parentView != null ? parentView.slug() : sanitizedSlug;
        model.addAttribute("categorySlug", resolvedSlug);
        model.addAttribute("children", childTiles);
        model.addAttribute("breadcrumbTrail", breadcrumb);
        model.addAttribute("products", cards);
        model.addAttribute("pagination", pagination);
        model.addAttribute("searchTotal", totalElements);
        model.addAttribute("selectedSizes", normalizedSizes);
        model.addAttribute("selectedColors", normalizedColors);
        model.addAttribute("selectedPriceRanges", priceRanges != null ? priceRanges : List.of());
        model.addAttribute("pageTitle", parentView != null && parentView.title() != null
                ? parentView.title() + " | X-Mate"
                : "Danh muc");

        return "client/category/index";
    }

    private Set<Long> collectDescendantIds(Long parentId) {
        List<Category> children = categoryService.findChildren(parentId);
        if (children == null || children.isEmpty()) return Set.of();
        Set<Long> ids = new LinkedHashSet<>();
        for (Category child : children) {
            ids.add(child.getId());
            ids.addAll(collectDescendantIds(child.getId()));
        }
        return ids;
    }

    private List<String> normalizeFilterValues(List<String> values) {
        if (values == null || values.isEmpty()) return List.of();
        return values.stream()
                .filter(v -> v != null && !v.isBlank())
                .map(v -> v.trim().toLowerCase(Locale.ROOT).replace(" ", ""))
                .distinct()
                .collect(Collectors.toList());
    }

    private List<PriceRange> parsePriceRanges(List<String> ranges) {
        if (ranges == null || ranges.isEmpty()) return List.of();
        List<PriceRange> result = new ArrayList<>();
        for (String range : ranges) {
            if (range == null) continue;
            switch (range.trim()) {
                case "0-200" -> result.add(new PriceRange(0, 200_000));
                case "200-300" -> result.add(new PriceRange(200_000, 300_000));
                case "300-500" -> result.add(new PriceRange(300_000, 500_000));
                case "500+" -> result.add(new PriceRange(500_000, Double.MAX_VALUE));
                default -> { }
            }
        }
        return result;
    }

    private boolean matchesSize(ProductCardView card, List<String> filters) {
        if (filters.isEmpty()) return true;
        List<String> cardSizes = card.sizes();
        if (cardSizes == null || cardSizes.isEmpty()) return false;
        Set<String> normalizedCardSizes = cardSizes.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(s -> s.trim().toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
        return filters.stream().anyMatch(normalizedCardSizes::contains);
    }

    private boolean matchesColor(ProductCardView card, List<String> filters) {
        if (filters.isEmpty()) return true;
        List<ProductColorView> colors = card.colors();
        if (colors == null || colors.isEmpty()) return false;
        Set<String> normalizedColors = colors.stream()
                .map(ProductColorView::name)
                .filter(name -> name != null && !name.isBlank())
                .map(name -> name.trim().toLowerCase(Locale.ROOT).replace(" ", ""))
                .collect(Collectors.toSet());
        return filters.stream().anyMatch(normalizedColors::contains);
    }

    private boolean matchesPrice(ProductCardView card, List<PriceRange> ranges) {
        if (ranges.isEmpty()) return true;
        double price = card.priceForCart();
        return ranges.stream().anyMatch(r -> price >= r.min && price <= r.max);
    }

    private record PriceRange(double min, double max) {}

    private List<CategoryDetailView> buildBreadcrumb(Category category) {
        List<CategoryDetailView> trail = new ArrayList<>();
        Category current = category;
        while (current != null) {
            trail.add(0, catalogViewService.toCategoryDetail(current));
            current = current.getParent();
        }
        return trail;
    }

    private void populateCategoryNotFound(Model model) {
        model.addAttribute("parent", null);
        model.addAttribute("category", null);
        model.addAttribute("categorySlug", null);
        model.addAttribute("children", List.of());
        model.addAttribute("products", List.of());
        model.addAttribute("pagination", null);
        model.addAttribute("searchTotal", 0);
        model.addAttribute("breadcrumbTrail", List.of());
        model.addAttribute("pageTitle", "Danh muc");
    }
}
