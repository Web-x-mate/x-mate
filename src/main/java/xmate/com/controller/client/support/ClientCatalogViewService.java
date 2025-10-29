package xmate.com.controller.client.support;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import xmate.com.controller.client.view.CategoryDetailView;
import xmate.com.controller.client.view.CategoryTileView;
import xmate.com.controller.client.view.NavColumn;
import xmate.com.controller.client.view.NavGroup;
import xmate.com.controller.client.view.NavItem;
import xmate.com.controller.client.view.PaginationView;
import xmate.com.controller.client.view.ProductCardView;
import xmate.com.controller.client.view.ProductColorView;
import xmate.com.controller.client.view.ProductDetailView;
import xmate.com.entity.catalog.Category;
import xmate.com.entity.catalog.Product;
import xmate.com.entity.catalog.ProductMedia;
import xmate.com.entity.catalog.ProductVariant;
import xmate.com.repo.catalog.ProductMediaRepository;
import xmate.com.repo.catalog.ProductVariantRepository;
import xmate.com.service.catalog.CategoryService;

import java.math.BigDecimal;
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
import java.util.stream.IntStream;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClientCatalogViewService {

    private static final Locale VI_VN = new Locale("vi", "VN");
    private static final Sort HOME_SORT = Sort.by("name").ascending();
    private static final Sort VARIANT_SORT = Sort.by("price").ascending();
    private static final int NAV_CATEGORY_LIMIT = 200;
    private static final int VARIANT_LIMIT = 200;
    private static final String PLACEHOLDER_IMAGE = "/images/product-placeholder.svg";
    private static final String MALE_SLUG = "nam";
    private static final String FEMALE_SLUG = "nu";

    private final CategoryService categoryService;
    private final ProductVariantRepository variantRepository;
    private final ProductMediaRepository mediaRepository;

    public List<NavGroup> buildPrimaryNav() {
        Page<Category> categoriesPage = categoryService.list(PageRequest.of(0, NAV_CATEGORY_LIMIT, HOME_SORT));
        List<Category> categories = categoriesPage.getContent();
        if (categories.isEmpty()) {
            return defaultNav();
        }

        Map<String, NavGroupBuilder> groups = new LinkedHashMap<>();

        // Pre-create groups if gender roots exist
        categories.stream()
                .filter(cat -> isGenderSlug(cat.getSlug()))
                .forEach(cat -> ensureGroup(groups, cat.getSlug(), cat.getName()));

        for (Category category : categories) {
            Category parent = category.getParent();
            if (parent == null) continue;

            String parentSlug = parent.getSlug();
            if (isGenderSlug(parentSlug)) {
                NavGroupBuilder group = ensureGroup(groups, parentSlug, parent.getName());
                if (group != null) {
                    group.ensureColumn(category.getSlug(), category.getName());
                }
                continue;
            }

            Category grandParent = parent.getParent();
            if (grandParent == null) continue;

            String grandSlug = grandParent.getSlug();
            if (isGenderSlug(grandSlug)) {
                NavGroupBuilder group = ensureGroup(groups, grandSlug, grandParent.getName());
                if (group != null) {
                    NavColumnBuilder column = group.ensureColumn(parent.getSlug(), parent.getName());
                    column.ensureItem(category.getSlug(), category.getName());
                }
            }
        }

        List<NavGroup> result = new ArrayList<>();
        NavGroupBuilder male = groups.get(MALE_SLUG);
        NavGroupBuilder female = groups.get(FEMALE_SLUG);
        if (male != null) {
            result.add(male.toView());
        }
        if (female != null) {
            result.add(female.toView());
        }

        if (result.isEmpty()) {
            return defaultNav();
        }
        return result;
    }

    public PaginationView buildPagination(Page<?> page) {
        if (page == null) {
            return new PaginationView(1, 0, 0, 0, false, false, 1, 1, List.of());
        }
        int totalPages = page.getTotalPages();
        int current = Math.max(1, page.getNumber() + 1);

        if (totalPages == 0) {
            return new PaginationView(
                    current,
                    page.getSize(),
                    0,
                    page.getTotalElements(),
                    false,
                    false,
                    current,
                    current,
                    List.of()
            );
        }

        int windowSize = 5;
        int start = Math.max(1, current - 2);
        int end = Math.min(totalPages, start + windowSize - 1);
        start = Math.max(1, end - windowSize + 1);

        List<Integer> pages = IntStream.rangeClosed(start, end).boxed().toList();

        int previous = page.hasPrevious() ? current - 1 : current;
        int next = page.hasNext() ? current + 1 : current;

        return new PaginationView(
                current,
                page.getSize(),
                totalPages,
                page.getTotalElements(),
                page.hasPrevious(),
                page.hasNext(),
                previous,
                next,
                pages
        );
    }

    public List<ProductCardView> toProductCards(List<Product> products) {
        if (products == null || products.isEmpty()) return List.of();
        return products.stream().map(this::toProductCard).toList();
    }

    public ProductCardView toProductCard(Product product) {
        ProductPresentation ctx = preparePresentation(product);
        return new ProductCardView(
                ctx.productId(),
                ctx.slug(),
                ctx.title(),
                ctx.finalPriceText(),
                ctx.originalPriceText(),
                ctx.hasDiscount(),
                ctx.discountPercent(),
                ctx.thumbnail(),
                ctx.hoverThumbnail(),
                ctx.priceForCart(),
                ctx.sizes(),
                ctx.colors()
        );
    }

    public ProductDetailView toProductDetail(Product product, String shareUrl) {
        ProductPresentation ctx = preparePresentation(product);
        List<String> images = ctx.medias().isEmpty()
                ? List.of(ctx.thumbnail())
                : ctx.medias().stream()
                    .map(ProductMedia::getUrl)
                    .filter(Objects::nonNull)
                    .toList();

        return new ProductDetailView(
                ctx.productId(),
                ctx.slug(),
                ctx.title(),
                product != null ? product.getDescription() : null,
                product != null && product.getCategory() != null ? product.getCategory().getName() : null,
                product != null && product.getCategory() != null ? product.getCategory().getSlug() : null,
                ctx.finalPriceText(),
                ctx.originalPriceText(),
                ctx.hasDiscount(),
                ctx.discountPercent(),
                ctx.thumbnail(),
                images.isEmpty() ? List.of(ctx.thumbnail()) : images,
                ctx.colors(),
                ctx.colors().isEmpty() ? null : ctx.colors().get(0),
                ctx.sizes(),
                ctx.sizes().isEmpty() ? null : ctx.sizes().get(0),
                ctx.priceForCart() >= 500_000,
                List.of(),
                ctx.priceForCart(),
                shareUrl
        );
    }

    public List<CategoryTileView> toCategoryTiles(List<Category> categories) {
        if (categories == null || categories.isEmpty()) return List.of();
        return categories.stream()
                .map(cat -> new CategoryTileView(
                        cat.getId(),
                        cat.getName(),
                        cat.getSlug(),
                        PLACEHOLDER_IMAGE,
                        null
                ))
                .toList();
    }

    public CategoryDetailView toCategoryDetail(Category category) {
        if (category == null) return null;
        return new CategoryDetailView(
                category.getId(),
                category.getName(),
                category.getSlug(),
                null
        );
    }

    private ProductPresentation preparePresentation(Product product) {
        if (product == null) {
            return ProductPresentation.empty();
        }

        Long productId = product.getId();
        List<ProductVariant> variants = productId == null
                ? List.of()
                : variantRepository.findAllByProduct_Id(
                        productId,
                        PageRequest.of(0, VARIANT_LIMIT, VARIANT_SORT)
                ).getContent();

        List<ProductMedia> medias = productId == null
                ? List.of()
                : mediaRepository.findAllByProduct_IdOrderBySortOrderAsc(productId);

        Map<Long, List<ProductMedia>> mediaByVariant = medias.stream()
                .filter(m -> m.getVariant() != null && m.getVariant().getId() != null)
                .collect(Collectors.groupingBy(m -> m.getVariant().getId(), LinkedHashMap::new, Collectors.toList()));

        Optional<ProductVariant> cheapest = variants.stream()
                .filter(v -> v.getPrice() != null)
                .min(Comparator.comparing(ProductVariant::getPrice));

        NumberFormat currency = NumberFormat.getCurrencyInstance(VI_VN);
        String finalPriceText = cheapest.map(ProductVariant::getPrice)
                .filter(Objects::nonNull)
                .map(currency::format)
                .orElse(null);
        String comparePriceText = cheapest.map(ProductVariant::getCompareAtPrice)
                .filter(Objects::nonNull)
                .map(currency::format)
                .orElse(null);

        boolean hasDiscount = finalPriceText != null && comparePriceText != null;
        int discountPercent = 0;
        if (hasDiscount) {
            BigDecimal p = cheapest.map(ProductVariant::getPrice).orElse(BigDecimal.ZERO);
            BigDecimal c = cheapest.map(ProductVariant::getCompareAtPrice).orElse(BigDecimal.ZERO);
            if (c.doubleValue() > 0) {
                discountPercent = (int) Math.round(100 - (p.doubleValue() / c.doubleValue() * 100));
            }
        }

        LinkedHashSet<String> allSizes = new LinkedHashSet<>();
        Map<String, Set<String>> sizesByColorHavingBarcode = new LinkedHashMap<>();

        for (ProductVariant variant : variants) {
            String size = Optional.ofNullable(variant.getSize()).orElse("").trim();
            String color = Optional.ofNullable(variant.getColor()).orElse("").trim();
            String barcode = Optional.ofNullable(variant.getBarcode()).orElse("").trim();

            if (!size.isEmpty()) allSizes.add(size);

            if (!color.isEmpty() && !size.isEmpty() && !barcode.isEmpty()) {
                sizesByColorHavingBarcode
                        .computeIfAbsent(color, k -> new LinkedHashSet<>())
                        .add(size);
            }
        }

        Map<String, ProductVariant> colorRep = new LinkedHashMap<>();
        variants.stream()
                .sorted(Comparator
                        .<ProductVariant>comparingInt(v -> mediaByVariant.getOrDefault(v.getId(), List.of()).isEmpty() ? 1 : 0)
                        .thenComparing(v -> Optional.ofNullable(v.getPrice()).orElse(BigDecimal.ZERO)))
                .forEach(v -> {
                    String color = Optional.ofNullable(v.getColor()).orElse("").trim();
                    if (!color.isEmpty() && !colorRep.containsKey(color)) {
                        colorRep.put(color, v);
                    }
                });

        String fallbackImage = medias.stream()
                .findFirst()
                .map(ProductMedia::getUrl)
                .orElse(PLACEHOLDER_IMAGE);

        List<ProductColorView> colorViews = new ArrayList<>();
        for (Map.Entry<String, ProductVariant> entry : colorRep.entrySet()) {
            String colorKey = entry.getKey();
            ProductVariant rep = entry.getValue();

            String hex = barcodeToHex(rep.getBarcode());
            List<ProductMedia> variantMedias = mediaByVariant.getOrDefault(rep.getId(), List.of());
            String imageUrl = variantMedias.stream()
                    .findFirst()
                    .map(ProductMedia::getUrl)
                    .orElse(fallbackImage);
            String hoverUrl = variantMedias.stream()
                    .skip(1)
                    .findFirst()
                    .map(ProductMedia::getUrl)
                    .orElse(null);

            List<String> sizesForColor = new ArrayList<>(sizesByColorHavingBarcode.getOrDefault(colorKey, Set.of()));
            String style = !hex.isEmpty() ? "--swatch-color:" + hex : "--swatch-color:#1f2937";

            colorViews.add(new ProductColorView(
                    colorKey,
                    hex,
                    null,
                    imageUrl,
                    hoverUrl,
                    rep.getId(),
                    style,
                    rep.getPrice(),
                    rep.getCompareAtPrice(),
                    sizesForColor
            ));

            log.info("[CLIENT] {} -> color='{}' repVariant={} sizes(withBarcode)={}",
                    Optional.ofNullable(product.getSlug()).orElse(String.valueOf(product.getId())),
                    colorKey, rep.getId(), sizesForColor);
        }

        String thumbnail = fallbackImage;
        String hoverThumbnail = null;
        if (!colorViews.isEmpty()) {
            ProductColorView firstColor = colorViews.get(0);
            thumbnail = Optional.ofNullable(firstColor.image()).orElse(fallbackImage);
            hoverThumbnail = firstColor.hoverImage();
        }

        String slug = Optional.ofNullable(product.getSlug())
                .filter(s -> !s.isBlank())
                .orElseGet(() -> "product-" + (productId != null ? productId : UUID.randomUUID()));
        String title = Optional.ofNullable(product.getName())
                .filter(s -> !s.isBlank())
                .orElse(slug);

        double priceForCart = cheapest.map(ProductVariant::getPrice)
                .map(BigDecimal::doubleValue)
                .orElse(0d);

        return new ProductPresentation(
                productId,
                slug,
                title,
                finalPriceText,
                comparePriceText,
                hasDiscount,
                discountPercent,
                thumbnail,
                hoverThumbnail,
                new ArrayList<>(allSizes),
                colorViews,
                medias,
                priceForCart
        );
    }

    private String barcodeToHex(String barcodeRaw) {
        String barcode = Optional.ofNullable(barcodeRaw).orElse("").trim();
        if (barcode.isEmpty()) return "";
        String normalized = barcode.startsWith("#") ? barcode.substring(1) : barcode;
        normalized = normalized.replaceAll("[^A-Fa-f0-9]", "");
        if (normalized.isEmpty()) return "";
        normalized = (normalized.length() >= 6)
                ? normalized.substring(0, 6)
                : String.format("%-6s", normalized).replace(' ', '0');
        return "#" + normalized.toUpperCase(Locale.ROOT);
    }

    private List<NavGroup> defaultNav() {
        return List.of(
                new NavGroup(MALE_SLUG, "NAM", List.of()),
                new NavGroup(FEMALE_SLUG, "NU", List.of())
        );
    }

    private boolean isGenderSlug(String slug) {
        if (slug == null) return false;
        String normalized = slug.trim().toLowerCase(Locale.ROOT);
        return MALE_SLUG.equals(normalized) || FEMALE_SLUG.equals(normalized);
    }

    private NavGroupBuilder ensureGroup(Map<String, NavGroupBuilder> groups, String slug, String title) {
        if (slug == null || slug.isBlank()) return null;
        String key = slug.trim().toLowerCase(Locale.ROOT);
        NavGroupBuilder builder = groups.computeIfAbsent(
                key,
                s -> new NavGroupBuilder(key, defaultTitleForSlug(key, title))
        );
        builder.updateTitle(title);
        return builder;
    }

    private String defaultTitleForSlug(String slug, String provided) {
        if (provided != null && !provided.isBlank()) return provided;
        if (MALE_SLUG.equals(slug)) return "NAM";
        if (FEMALE_SLUG.equals(slug)) return "NU";
        return slug;
    }

    private static final class NavGroupBuilder {
        private final String slug;
        private String title;
        private final LinkedHashMap<String, NavColumnBuilder> columns = new LinkedHashMap<>();

        private NavGroupBuilder(String slug, String title) {
            this.slug = slug;
            this.title = title;
        }

        private void updateTitle(String maybeTitle) {
            if (maybeTitle != null && !maybeTitle.isBlank()) {
                this.title = maybeTitle;
            }
        }

        private NavColumnBuilder ensureColumn(String slug, String title) {
            String fallbackSlug = (slug == null || slug.isBlank())
                    ? UUID.randomUUID().toString()
                    : slug;
            String key = fallbackSlug.trim().toLowerCase(Locale.ROOT);
            NavColumnBuilder column = columns.computeIfAbsent(
                    key,
                    s -> new NavColumnBuilder(fallbackSlug, title)
            );
            column.updateTitle(title);
            return column;
        }

        private NavGroup toView() {
            List<NavColumn> viewColumns = columns.values().stream()
                    .map(NavColumnBuilder::toView)
                    .toList();
            return new NavGroup(slug, title, viewColumns);
        }
    }

    private static final class NavColumnBuilder {
        private final String slug;
        private String title;
        private final LinkedHashMap<String, NavItem> items = new LinkedHashMap<>();

        private NavColumnBuilder(String slug, String title) {
            this.slug = slug;
            this.title = title;
        }

        private void updateTitle(String maybeTitle) {
            if (maybeTitle != null && !maybeTitle.isBlank()) {
                this.title = maybeTitle;
            }
        }

        private void ensureItem(String slug, String title) {
            if (slug == null || slug.isBlank()) return;
            items.computeIfAbsent(
                    slug,
                    s -> new NavItem(slug, title != null && !title.isBlank() ? title : slug)
            );
        }

        private NavColumn toView() {
            return new NavColumn(slug, title, new ArrayList<>(items.values()));
        }
    }

    private record ProductPresentation(
            Long productId,
            String slug,
            String title,
            String finalPriceText,
            String originalPriceText,
            boolean hasDiscount,
            int discountPercent,
            String thumbnail,
            String hoverThumbnail,
            List<String> sizes,
            List<ProductColorView> colors,
            List<ProductMedia> medias,
            double priceForCart
    ) {
        static ProductPresentation empty() {
            return new ProductPresentation(
                    null,
                    "product",
                    "Sản phẩm",
                    null,
                    null,
                    false,
                    0,
                    PLACEHOLDER_IMAGE,
                    null,
                    List.of(),
                    List.of(),
                    List.of(),
                    0d
            );
        }
    }
}
