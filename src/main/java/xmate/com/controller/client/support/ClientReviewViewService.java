package xmate.com.controller.client.support;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import xmate.com.controller.client.view.ReviewItemView;
import xmate.com.entity.catalog.Product;
import xmate.com.entity.catalog.ProductMedia;
import xmate.com.entity.catalog.ProductVariant;
import xmate.com.entity.common.ReviewStatus;
import xmate.com.entity.customer.Customer;
import xmate.com.entity.enums.OrderStatus;
import xmate.com.entity.sales.Order;
import xmate.com.entity.sales.OrderItem;
import xmate.com.repo.catalog.ProductMediaRepository;
import xmate.com.repo.catalog.ProductVariantRepository;
import xmate.com.repo.review.ProductReviewRepository;
import xmate.com.repo.sales.OrderRepository;

import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClientReviewViewService {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String PLACEHOLDER_IMAGE = "/client/images/product-placeholder.svg";

    private final OrderRepository orderRepository;
    private final ProductMediaRepository mediaRepository;
    private final ProductReviewRepository reviewRepository;
    private final ProductVariantRepository variantRepository;

    @Transactional(readOnly = true)
    public List<ReviewItemView> build(Customer customer) {
        if (customer == null) return List.of();
        List<Order> orders = orderRepository.findTop30ByCustomerOrderByCreatedAtDesc(customer);
        if (orders.isEmpty()) return List.of();

        Map<Long, String> variantThumbCache = new HashMap<>();
        Map<Long, String> productThumbCache = new HashMap<>();
        List<ReviewItemView> items = new ArrayList<>();

        for (Order o : orders) {
            if (o.getStatus() != OrderStatus.DELIVERED) continue;
            if (o.getItems() == null) continue;
            String orderDate = o.getCreatedAt() != null ? o.getCreatedAt().format(DATE_TIME) : null;
            for (OrderItem it : o.getItems()) {
                if (it == null) continue;
                Product product = it.getProduct();
                ProductVariant variant = it.getVariant();

                String vProductName = null;
                String vSlug = null;
                if (variant != null) {
                    try {
                        if (variant.getProduct() != null) {
                            vProductName = trimToNull(variant.getProduct().getName());
                            vSlug = trimToNull(variant.getProduct().getSlug());
                        } else if (variant.getId() != null) {
                            ProductVariant v = variantRepository.findById(variant.getId()).orElse(null);
                            if (v != null && v.getProduct() != null) {
                                vProductName = trimToNull(v.getProduct().getName());
                                vSlug = trimToNull(v.getProduct().getSlug());
                            }
                        }
                    } catch (Exception ignored) {}
                }

                String name = firstNonBlank(
                        it.getProductName(),
                        product != null ? trimToNull(product.getName()) : null,
                        vProductName,
                        "Sản phẩm"
                );
                String slug = (product != null) ? trimToNull(product.getSlug()) : vSlug;

                String color = variant != null ? trimToNull(variant.getColor()) : null;
                String size  = variant != null ? trimToNull(variant.getSize()) : null;
                String thumb = resolveThumbnail(product, variant, variantThumbCache, productThumbCache);

                var existing = reviewRepository.findByCustomer_IdAndOrderItemId(customer.getId(), it.getId()).orElse(null);
                Integer existingRating = existing != null ? existing.getRating() : null;
                String existingContent = existing != null ? existing.getContent() : null;
                String statusLabel = null;
                String statusClass = null;
                boolean canReview = true;
                if (existing != null) {
                    canReview = false;
                    if (existing.getStatus() == ReviewStatus.PENDING) {
                        statusLabel = "CHỜ XÁC NHẬN";
                        statusClass = "is-pending";
                    } else if (existing.getStatus() == ReviewStatus.APPROVED) {
                        statusLabel = "HOÀN TẤT";
                        statusClass = "is-done";
                    } else {
                        statusLabel = "BỊ TỪ CHỐI";
                        statusClass = "is-rejected";
                    }
                }

                items.add(new ReviewItemView(
                        it.getId(),
                        o.getCode(),
                        orderDate,
                        product != null ? product.getId() : null,
                        variant != null ? variant.getId() : null,
                        name,
                        slug,
                        thumb,
                        color,
                        size,
                        existingRating,
                        existingContent,
                        statusLabel,
                        statusClass,
                        canReview
                ));
            }
        }
        return items;
    }

    private String resolveThumbnail(Product product,
                                    ProductVariant variant,
                                    Map<Long, String> variantThumbCache,
                                    Map<Long, String> productThumbCache) {
        if (variant != null && variant.getId() != null) {
            String cached = variantThumbCache.get(variant.getId());
            if (cached != null) return cached;
            String url = loadFirstMediaUrl(() -> mediaRepository.findAllByVariant_IdOrderBySortOrderAsc(variant.getId()));
            if (url != null) {
                variantThumbCache.put(variant.getId(), url);
                return url;
            }
        }
        if (product != null && product.getId() != null) {
            String cached = productThumbCache.get(product.getId());
            if (cached != null) return cached;
            String url = loadFirstMediaUrl(() -> mediaRepository.findAllByProduct_IdOrderBySortOrderAsc(product.getId()));
            if (url != null) {
                productThumbCache.put(product.getId(), url);
                return url;
            }
        }
        return PLACEHOLDER_IMAGE;
    }

    private String loadFirstMediaUrl(java.util.function.Supplier<List<ProductMedia>> supplier) {
        try {
            return supplier.get().stream()
                    .filter(Objects::nonNull)
                    .map(ProductMedia::getUrl)
                    .filter(u -> u != null && !u.isBlank())
                    .findFirst().orElse(null);
        } catch (Exception e) {
            log.warn("Failed to load product media", e);
            return null;
        }
    }

    private String firstNonBlank(String... values) {
        if (values == null) return null;
        for (String v : values) {
            if (v != null && !v.trim().isEmpty()) return v.trim();
        }
        return null;
    }

    private String trimToNull(String v) {
        if (v == null) return null;
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }
}
