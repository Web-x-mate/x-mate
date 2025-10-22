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
import xmate.com.entity.catalog.Category;
import xmate.com.entity.catalog.Product;
import xmate.com.entity.catalog.ProductMedia;
import xmate.com.entity.catalog.ProductVariant;
import xmate.com.repo.catalog.ProductMediaRepository;
import xmate.com.repo.catalog.ProductVariantRepository;
import xmate.com.service.catalog.CategoryService;
import xmate.com.service.catalog.ProductService;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ClientHomeController {

    private final ProductService productService;
    private final CategoryService categoryService;
    private final ProductVariantRepository variantRepository;
    private final ProductMediaRepository mediaRepository;

    @GetMapping({"/", "/home"})
    public String home(@RequestParam(name = "q", required = false) String query,
                       Model model) {

        boolean isSearch = query != null && !query.isBlank();
        Page<Product> page = isSearch
                ? productService.search(query, PageRequest.of(0, 12, Sort.by("createdAt").descending()))
                : productService.list(PageRequest.of(0, 12, Sort.by("createdAt").descending()));

        log.info("[HOME] query='{}', isSearch={}, fetched {} products (page size={})",
                query, isSearch, page.getNumberOfElements(), page.getSize());

        List<ProductCardView> cards = page.getContent().stream()
                .map(this::toProductCard)
                .toList();

        if (cards.isEmpty()) {
            log.warn("[HOME] No products available to render.");
        } else {
            log.info("[HOME] First card: {}", cards.get(0));
        }

        model.addAttribute("pageTitle", "X-Mate | Trang chu");
        model.addAttribute("isSearch", isSearch);
        model.addAttribute("searchQuery", query);
        model.addAttribute("searchTotal", page.getTotalElements());
        model.addAttribute("products", cards);
        model.addAttribute("primaryCategories", buildNavGroups());
        model.addAttribute("cartQuantity", 0);

        return "client/home/index";
    }

    private List<NavGroup> buildNavGroups() {
        Page<Category> categoriesPage = categoryService.list(PageRequest.of(0, 200, Sort.by("name").ascending()));
        List<Category> categories = categoriesPage.getContent();

        Map<Long, List<Category>> byParent = categories.stream()
                .filter(c -> c.getParent() != null)
                .collect(Collectors.groupingBy(c -> c.getParent().getId()));

        List<Category> roots = categories.stream()
                .filter(c -> c.getParent() == null)
                .sorted(Comparator.comparing(Category::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();

        if (roots.isEmpty()) {
            return List.of(
                    new NavGroup("nam", "NAM", List.of()),
                    new NavGroup("nu", "NU", List.of())
            );
        }

        List<NavGroup> groups = new ArrayList<>();
        for (Category root : roots) {
            List<NavColumn> columns = byParent.getOrDefault(root.getId(), List.of()).stream()
                    .sorted(Comparator.comparing(Category::getName, String.CASE_INSENSITIVE_ORDER))
                    .map(child -> {
                        List<NavItem> items = byParent.getOrDefault(child.getId(), List.of()).stream()
                                .sorted(Comparator.comparing(Category::getName, String.CASE_INSENSITIVE_ORDER))
                                .map(c -> new NavItem(c.getSlug(), c.getName()))
                                .toList();
                        return new NavColumn(child.getSlug(), child.getName(), items);
                    })
                    .toList();
            groups.add(new NavGroup(root.getSlug(), root.getName(), columns));
        }
        return groups;
    }

    private ProductCardView toProductCard(Product product) {
        List<ProductVariant> variants = variantRepository
                .findAllByProduct_Id(product.getId(),
                        PageRequest.of(0, 50, Sort.by("price").ascending()))
                .getContent();

        Optional<ProductVariant> cheapest = variants.stream()
                .filter(v -> v.getPrice() != null)
                .min(Comparator.comparing(ProductVariant::getPrice));

        NumberFormat currency = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        String finalPriceText = cheapest
                .map(ProductVariant::getPrice)
                .filter(Objects::nonNull)
                .map(currency::format)
                .orElse(null);

        String comparePriceText = cheapest
                .map(ProductVariant::getCompareAtPrice)
                .filter(Objects::nonNull)
                .map(currency::format)
                .orElse(null);

        boolean hasDiscount = finalPriceText != null && comparePriceText != null;
        int discountPercent = 0;
        if (hasDiscount) {
            var price = cheapest.get().getPrice();
            var compare = cheapest.get().getCompareAtPrice();
            if (price != null && compare != null && compare.doubleValue() > 0) {
                discountPercent = (int) Math.round(100 - (price.doubleValue() / compare.doubleValue() * 100));
            }
        }

        List<ProductMedia> medias = mediaRepository.findAllByProduct_IdOrderBySortOrderAsc(product.getId());
        Map<Long, List<ProductMedia>> mediaByVariant = medias.stream()
                .filter(media -> media.getVariant() != null)
                .collect(Collectors.groupingBy(media -> media.getVariant().getId()));

        String fallbackImage = medias.stream()
                .findFirst()
                .map(ProductMedia::getUrl)
                .orElse("/images/product-placeholder.svg");

        List<String> sizes = new ArrayList<>();
        Set<String> seenSizes = new LinkedHashSet<>();
        Map<String, Map<String, Object>> colorMap = new LinkedHashMap<>();

        for (ProductVariant variant : variants) {
            if (variant.getSize() != null && !variant.getSize().isBlank() && seenSizes.add(variant.getSize())) {
                sizes.add(variant.getSize());
            }

            String colorKey = Optional.ofNullable(variant.getColor()).orElse("").trim();
            if (!colorKey.isEmpty() && !colorMap.containsKey(colorKey)) {
                String barcode = Optional.ofNullable(variant.getBarcode()).orElse("").trim();
                String hex = "";
                if (!barcode.isEmpty()) {
                    String normalized = barcode.startsWith("#") ? barcode.substring(1) : barcode;
                    normalized = normalized.replaceAll("[^A-Fa-f0-9]", "");
                    if (!normalized.isEmpty()) {
                        normalized = normalized.length() >= 6 ? normalized.substring(0, 6) : String.format("%-6s", normalized).replace(' ', '0');
                        hex = "#" + normalized.toUpperCase(Locale.ROOT);
                    }
                }

                String imageUrl = mediaByVariant.getOrDefault(variant.getId(), List.of()).stream()
                        .findFirst()
                        .map(ProductMedia::getUrl)
                        .orElse(fallbackImage);

                String style;
                if (!hex.isEmpty()) {
                    style = "--swatch-color:" + hex;
                } else {
                    style = "--swatch-color:#1f2937";
                }

                Map<String, Object> color = new LinkedHashMap<>();
                color.put("name", colorKey);
                color.put("hex", hex);
                color.put("swatchUrl", null);
                color.put("image", imageUrl);
                color.put("style", style);
                colorMap.put(colorKey, color);
            }
        }

        String slug = Optional.ofNullable(product.getSlug())
                .filter(s -> !s.isBlank())
                .orElseGet(() -> "product-" + (product.getId() != null ? product.getId() : UUID.randomUUID()));

        String title = Optional.ofNullable(product.getName())
                .filter(s -> !s.isBlank())
                .orElse(slug);

        String thumbnail = !colorMap.isEmpty()
                ? colorMap.values().iterator().next().getOrDefault("image", fallbackImage).toString()
                : fallbackImage;

        ProductCardView card = new ProductCardView(
                slug,
                title,
                finalPriceText,
                comparePriceText,
                hasDiscount,
                discountPercent,
                thumbnail,
                null,
                cheapest.map(ProductVariant::getPrice).map(Number::doubleValue).orElse(0d),
                sizes,
                new ArrayList<>(colorMap.values())
        );

        log.info("[HOME] Card ready slug={} title='{}' price={} image={} sizes={} colors={}",
                card.slug(), card.title(), card.finalPriceText(), card.thumbnail(), sizes, colorMap.keySet());

        return card;
    }

    public record NavGroup(String slug, String title, List<NavColumn> children) {
    }

    public record NavColumn(String slug, String title, List<NavItem> items) {
    }

    public record NavItem(String slug, String title) {
    }

    public record ProductCardView(
            String slug,
            String title,
            String finalPriceText,
            String originalPriceText,
            boolean hasDiscount,
            int discountPercent,
            String thumbnail,
            String hoverThumbnail,
            double priceForCart,
            List<String> sizes,
            List<Map<String, Object>> colors
    ) {
        @Override
        public String toString() {
            String colorNames = colors.stream()
                    .map(c -> Objects.toString(c.get("name"), ""))
                    .collect(Collectors.joining(","));
            return "ProductCardView{" +
                    "slug='" + slug + '\'' +
                    ", title='" + title + '\'' +
                    ", finalPriceText='" + finalPriceText + '\'' +
                    ", thumbnail='" + thumbnail + '\'' +
                    ", sizes=" + sizes +
                    ", colors=[" + colorNames + "]" +
                    '}';
        }
    }
}
