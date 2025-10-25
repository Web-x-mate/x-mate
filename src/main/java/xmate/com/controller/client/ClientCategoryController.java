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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import xmate.com.controller.client.support.ClientCatalogViewService;
import xmate.com.controller.client.view.CategoryDetailView;
import xmate.com.controller.client.view.CategoryTileView;
import xmate.com.controller.client.view.PaginationView;
import xmate.com.controller.client.view.ProductCardView;
import xmate.com.entity.catalog.Category;
import xmate.com.entity.catalog.Product;
import xmate.com.service.catalog.CategoryService;
import xmate.com.service.catalog.ProductService;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
                         Model model) {
        return renderCategoryPage(slug, pageParam, sizeParam, model);
    }

    @GetMapping({"/nam", "/nu"})
    public String genderCollections(HttpServletRequest request,
                                    @RequestParam(name = "page", defaultValue = "1") int pageParam,
                                    @RequestParam(name = "size", defaultValue = "12") int sizeParam,
                                    Model model) {
        String uri = request.getRequestURI();
        String slug = uri != null && uri.toLowerCase().contains("/nu") ? FEMALE_SLUG : MALE_SLUG;
        return renderCategoryPage(slug, pageParam, sizeParam, model);
    }

    private String renderCategoryPage(String slug,
                                      int pageParam,
                                      int sizeParam,
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

        Set<Long> categoryIds = new LinkedHashSet<>();
        categoryIds.add(parent.getId());
        categoryIds.addAll(collectDescendantIds(parent.getId()));

        PageRequest pageRequest = PageRequest.of(sanitizedPage - 1, sanitizedSize, CATEGORY_SORT);
        Page<Product> productPage = productService.byCategories(categoryIds, pageRequest);
        if (sanitizedPage > 1 && productPage.getTotalPages() > 0 && sanitizedPage > productPage.getTotalPages()) {
            int lastPageIdx = Math.max(productPage.getTotalPages() - 1, 0);
            productPage = productService.byCategories(
                    categoryIds,
                    PageRequest.of(lastPageIdx, sanitizedSize, CATEGORY_SORT)
            );
        }

        List<ProductCardView> cards = catalogViewService.toProductCards(productPage.getContent());
        PaginationView pagination = catalogViewService.buildPagination(productPage);

        model.addAttribute("parent", parentView);
        model.addAttribute("category", parentView);
        model.addAttribute("categorySlug", parentView != null ? parentView.slug() : sanitizedSlug);
        model.addAttribute("children", childTiles);
        model.addAttribute("products", cards);
        model.addAttribute("pagination", pagination);
        model.addAttribute("searchTotal", productPage.getTotalElements());
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

    private void populateCategoryNotFound(Model model) {
        model.addAttribute("parent", null);
        model.addAttribute("category", null);
        model.addAttribute("categorySlug", null);
        model.addAttribute("children", List.of());
        model.addAttribute("products", List.of());
        model.addAttribute("pagination", null);
        model.addAttribute("searchTotal", 0);
        model.addAttribute("pageTitle", "Danh muc");
    }
}
