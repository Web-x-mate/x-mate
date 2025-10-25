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
import java.math.BigDecimal;

import java.text.NumberFormat;
import java.util.*;
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

        if (cards.isEmpty()) log.warn("[HOME] No products available to render.");
        else log.info("[HOME] First card: {}", cards.get(0));

        model.addAttribute("pageTitle", "X-Mate | Trang chủ");
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

        log.debug("[NAV] total categories = {}", categories.size());
        log.debug("[NAV] roots = {}", roots.stream().map(Category::getName).toList());

        if (roots.isEmpty()) {
            return List.of(
                    new NavGroup("nam", "NAM", List.of()),
                    new NavGroup("nu", "NỮ", List.of())
            );
        }

        List<NavGroup> groups = new ArrayList<>();
        for (Category root : roots) {
            List<Category> children = byParent.getOrDefault(root.getId(), List.of()).stream()
                    .sorted(Comparator.comparing(Category::getName, String.CASE_INSENSITIVE_ORDER))
                    .toList();

            log.debug("[NAV] root '{}' has {} children -> {}",
                    root.getName(), children.size(), children.stream().map(Category::getName).toList());

            List<NavColumn> columns = children.stream()
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

    // 1) Lấy variants (giá tăng dần cho “rẻ nhất”)
    List<ProductVariant> variants = variantRepository
        .findAllByProduct_Id(product.getId(),
            PageRequest.of(0, 200, Sort.by("price").ascending()))
        .getContent();

    // 2) Tính giá hiển thị (dùng variant rẻ nhất)
    Optional<ProductVariant> cheapest = variants.stream()
        .filter(v -> v.getPrice() != null)
        .min(Comparator.comparing(ProductVariant::getPrice));

    NumberFormat currency = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
    String finalPriceText = cheapest.map(ProductVariant::getPrice)
        .filter(Objects::nonNull).map(currency::format).orElse(null);
    String comparePriceText = cheapest.map(ProductVariant::getCompareAtPrice)
        .filter(Objects::nonNull).map(currency::format).orElse(null);

    boolean hasDiscount = finalPriceText != null && comparePriceText != null;
    int discountPercent = 0;
    if (hasDiscount) {
        var p = cheapest.get().getPrice();
        var c = cheapest.get().getCompareAtPrice();
        if (p != null && c != null && c.doubleValue() > 0) {
            discountPercent = (int)Math.round(100 - (p.doubleValue() / c.doubleValue() * 100));
        }
    }

    // 3) Medias
    List<ProductMedia> medias = mediaRepository.findAllByProduct_IdOrderBySortOrderAsc(product.getId());
    Map<Long, List<ProductMedia>> mediaByVariant = medias.stream()
        .filter(m -> m.getVariant() != null)
        .collect(Collectors.groupingBy(m -> m.getVariant().getId()));
    String fallbackImage = medias.stream().findFirst()
        .map(ProductMedia::getUrl).orElse("/images/product-placeholder.svg");

    // 4) Gom size theo MÀU **chỉ lấy variant có barcode**
    Map<String, Set<String>> sizesByColorHavingBarcode = new LinkedHashMap<>();
    // Đồng thời gom tất cả size (để render nút), có thể lấy theo barcode hoặc tất cả tùy bạn:
    LinkedHashSet<String> allSizes = new LinkedHashSet<>();

    for (ProductVariant v : variants) {
        String size  = Optional.ofNullable(v.getSize()).orElse("").trim();
        String color = Optional.ofNullable(v.getColor()).orElse("").trim();
        String bc    = Optional.ofNullable(v.getBarcode()).orElse("").trim();

        if (!size.isEmpty()) allSizes.add(size);

        if (!color.isEmpty() && !size.isEmpty() && !bc.isEmpty()) {
            sizesByColorHavingBarcode
                .computeIfAbsent(color, k -> new LinkedHashSet<>())
                .add(size);
        }
    }

    // 5) Chọn variant “đại diện” mỗi màu: ưu tiên có media, sau đó giá rẻ
    Map<String, ProductVariant> colorRep = new LinkedHashMap<>();
    variants.stream()
        .sorted(Comparator
            .<ProductVariant>comparingInt(v -> mediaByVariant.getOrDefault(v.getId(), List.of()).isEmpty() ? 1 : 0)
            .thenComparing(v -> Optional.ofNullable(v.getPrice()).orElse(BigDecimal.ZERO)))
        .forEach(v -> {
            String color = Optional.ofNullable(v.getColor()).orElse("").trim();
            if (!color.isEmpty() && !colorRep.containsKey(color)) colorRep.put(color, v);
        });

    // 6) Build color map
    Map<String, Map<String,Object>> colorMap = new LinkedHashMap<>();
    for (Map.Entry<String, ProductVariant> e : colorRep.entrySet()) {
        String colorKey = e.getKey();
        ProductVariant rep = e.getValue();

        // HEX từ barcode
        String hex = "";
        String barcode = Optional.ofNullable(rep.getBarcode()).orElse("").trim();
        if (!barcode.isEmpty()) {
            String n = barcode.startsWith("#") ? barcode.substring(1) : barcode;
            n = n.replaceAll("[^A-Fa-f0-9]", "");
            if (!n.isEmpty()) {
                n = (n.length() >= 6) ? n.substring(0, 6)
                        : String.format("%-6s", n).replace(' ', '0');
                hex = "#" + n.toUpperCase(Locale.ROOT);
            }
        }

        // Ảnh đại diện màu
        List<ProductMedia> vMedias = mediaByVariant.getOrDefault(rep.getId(), List.of());
        String imageUrl = vMedias.stream().findFirst().map(ProductMedia::getUrl).orElse(fallbackImage);
        String hoverUrl = vMedias.stream().skip(1).findFirst().map(ProductMedia::getUrl).orElse(null);

        // Sizes hợp lệ cho màu (chỉ từ variant có barcode)
        List<String> sizesForColor = new ArrayList<>(sizesByColorHavingBarcode.getOrDefault(colorKey, Set.of()));

        Map<String,Object> color = new LinkedHashMap<>();
        color.put("name", colorKey);
        color.put("hex", hex);
        color.put("swatchUrl", null);
        color.put("image", imageUrl);
        color.put("hoverImage", hoverUrl);
        color.put("variantId", rep.getId());
        color.put("style", !hex.isEmpty() ? "--swatch-color:" + hex : "--swatch-color:#1f2937");
        color.put("price", rep.getPrice());
        color.put("compareAt", rep.getCompareAtPrice());
        color.put("sizes", sizesForColor);

        colorMap.put(colorKey, color);

        // ===== LOG: size theo từng màu =====
        log.info("[HOME] {} -> color='{}' repVariant={} sizes(withBarcode)={}",
            Optional.ofNullable(product.getSlug()).orElse(String.valueOf(product.getId())),
            colorKey, rep.getId(), sizesForColor);
    }

    // 7) Thông tin chung cho card
    String slug = Optional.ofNullable(product.getSlug()).filter(s -> !s.isBlank())
        .orElseGet(() -> "product-" + (product.getId() != null ? product.getId() : UUID.randomUUID()));
    String title = Optional.ofNullable(product.getName()).filter(s -> !s.isBlank()).orElse(slug);

    String thumbnail = fallbackImage;
    String hoverThumbnail = null;
    if (!colorMap.isEmpty()) {
        Map<String,Object> firstColor = colorMap.values().iterator().next();
        thumbnail = Objects.toString(firstColor.getOrDefault("image", fallbackImage), fallbackImage);
        hoverThumbnail = Optional.ofNullable(firstColor.get("hoverImage")).map(Object::toString).orElse(null);
    }

    ProductCardView card = new ProductCardView(
        slug,
        title,
        finalPriceText,
        comparePriceText,
        hasDiscount,
        discountPercent,
        thumbnail,
        hoverThumbnail,
        cheapest.map(ProductVariant::getPrice).map(Number::doubleValue).orElse(0d),
        new ArrayList<>(allSizes),                 // render tất cả nút size
        new ArrayList<>(colorMap.values())         // mỗi color có "sizes"
    );

    log.info("[HOME] Card ready slug={} sizesAll={} colors={}",
        card.slug(), card.sizes(), colorMap.keySet());

    return card;
}



    // ==== DTO/records for view ====
    public record NavGroup(String slug, String title, List<NavColumn> children) {}
    public record NavColumn(String slug, String title, List<NavItem> items) {}
    public record NavItem(String slug, String title) {}

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
