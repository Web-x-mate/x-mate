package xmate.com.controller.client.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import xmate.com.controller.client.view.OrderHistoryView;
import xmate.com.entity.catalog.Product;
import xmate.com.entity.catalog.ProductMedia;
import xmate.com.entity.catalog.ProductVariant;
import xmate.com.entity.customer.Customer;
import xmate.com.entity.enums.OrderStatus;
import xmate.com.entity.enums.PaymentStatus;
import xmate.com.entity.sales.Order;
import xmate.com.entity.sales.OrderItem;
import xmate.com.repo.catalog.ProductMediaRepository;
import xmate.com.repo.sales.OrderRepository;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClientOrderViewService {

    private static final Locale VI_VN = new Locale("vi", "VN");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String PLACEHOLDER_IMAGE = "/client/images/product-placeholder.svg";

    private final OrderRepository orderRepository;
    private final ProductMediaRepository mediaRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<OrderHistoryView> buildOrderHistory(Customer customer) {
        if (customer == null) {
            return List.of();
        }
        List<Order> orders = orderRepository.findTop30ByCustomerOrderByCreatedAtDesc(customer);
        if (orders.isEmpty()) {
            return List.of();
        }

        Map<Long, String> variantThumbCache = new HashMap<>();
        Map<Long, String> productThumbCache = new HashMap<>();

        return orders.stream()
                .map(order -> toView(order, variantThumbCache, productThumbCache))
                .toList();
    }

    private OrderHistoryView toView(Order order,
                                    Map<Long, String> variantThumbCache,
                                    Map<Long, String> productThumbCache) {
        String code = nvl(order.getCode(), "-");
        String shortCode = code.length() > 8 ? code.substring(code.length() - 8) : code;

        String statusClass = resolveStatusClass(order.getStatus(), order.getPaymentStatus());
        String statusLabel = resolveStatusLabel(order.getStatus());
        String paymentLabel = resolvePaymentLabel(order.getPaymentStatus());

        String createdAtText = order.getCreatedAt() != null
                ? order.getCreatedAt().format(DATE_TIME)
                : null;

        List<OrderHistoryView.Item> itemViews = order.getItems() == null
                ? List.of()
                : order.getItems().stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(OrderItem::getId, Comparator.nullsLast(Long::compareTo)))
                .map(item -> toItemView(item, variantThumbCache, productThumbCache))
                .toList();

        int quantityTotal = itemViews.stream().mapToInt(OrderHistoryView.Item::quantity).sum();

        OrderHistoryView.Shipping shipping = buildShipping(order.getShippingAddress());
        OrderHistoryView.Totals totals = new OrderHistoryView.Totals(
                formatCurrency(order.getSubtotal()),
                formatCurrency(order.getDiscountAmount()),
                formatCurrency(order.getShippingFee()),
                formatCurrency(order.getTotal())
        );

        boolean hasDiscount = order.getDiscountAmount() > 0L;
        String note = trimToNull(order.getNoteInternal());

        return new OrderHistoryView(
                code,
                shortCode,
                statusClass,
                statusLabel,
                paymentLabel,
                createdAtText,
                quantityTotal,
                itemViews,
                shipping,
                totals,
                hasDiscount,
                note
        );
    }

    private OrderHistoryView.Item toItemView(OrderItem item,
                                             Map<Long, String> variantThumbCache,
                                             Map<Long, String> productThumbCache) {
        Product product = item.getProduct();
        ProductVariant variant = item.getVariant();

        String title = firstNonBlank(
                item.getProductName(),
                product != null ? product.getName() : null,
                "Sản phẩm"
        );

        String slug = product != null ? trimToNull(product.getSlug()) : null;
        String thumbnail = resolveThumbnail(product, variant, variantThumbCache, productThumbCache);
        String color = variant != null ? trimToNull(variant.getColor()) : null;
        String size = variant != null ? trimToNull(variant.getSize()) : null;

        String priceText = formatCurrency(item.getPrice());
        String lineTotalText = formatCurrency(item.getLineTotal());

        return new OrderHistoryView.Item(
                thumbnail,
                title,
                slug,
                color,
                size,
                priceText,
                item.getQty(),
                lineTotalText
        );
    }

    private OrderHistoryView.Shipping buildShipping(String shippingJson) {
        if (shippingJson == null || shippingJson.isBlank()) {
            return new OrderHistoryView.Shipping("Chưa cập nhật", null, "Chưa cập nhật");
        }
        try {
            JsonNode node = objectMapper.readTree(shippingJson);
            String name = textOrNull(node, "fullName");
            String phone = textOrNull(node, "phone");
            String fullAddress = textOrNull(node, "fullAddress");

            if (fullAddress == null || fullAddress.isBlank()) {
                fullAddress = joinAddressParts(
                        textOrNull(node, "addressLine"),
                        textOrNull(node, "ward"),
                        textOrNull(node, "district"),
                        textOrNull(node, "province")
                );
            }

            if (name == null || name.isBlank()) {
                name = "Chưa cập nhật";
            }
            if (fullAddress == null || fullAddress.isBlank()) {
                fullAddress = "Chưa cập nhật";
            }

            return new OrderHistoryView.Shipping(name, phone, fullAddress);
        } catch (Exception ex) {
            log.warn("Cannot parse shipping address JSON: {}", shippingJson, ex);
            return new OrderHistoryView.Shipping("Chưa cập nhật", null, "Chưa cập nhật");
        }
    }

    private String resolveThumbnail(Product product,
                                    ProductVariant variant,
                                    Map<Long, String> variantThumbCache,
                                    Map<Long, String> productThumbCache) {
        if (variant != null && variant.getId() != null) {
            String cached = variantThumbCache.get(variant.getId());
            if (cached != null) {
                return cached;
            }
            String resolved = loadFirstMediaUrl(() ->
                    mediaRepository.findAllByVariant_IdOrderBySortOrderAsc(variant.getId())
            );
            if (resolved != null) {
                variantThumbCache.put(variant.getId(), resolved);
                return resolved;
            }
        }

        if (product != null && product.getId() != null) {
            String cached = productThumbCache.get(product.getId());
            if (cached != null) {
                return cached;
            }
            String resolved = loadFirstMediaUrl(() ->
                    mediaRepository.findAllByProduct_IdOrderBySortOrderAsc(product.getId())
            );
            if (resolved != null) {
                productThumbCache.put(product.getId(), resolved);
                return resolved;
            }
        }

        return PLACEHOLDER_IMAGE;
    }

    private String loadFirstMediaUrl(Supplier<List<ProductMedia>> supplier) {
        try {
            return supplier.get().stream()
                    .filter(Objects::nonNull)
                    .map(ProductMedia::getUrl)
                    .filter(url -> url != null && !url.isBlank())
                    .findFirst()
                    .orElse(null);
        } catch (Exception ex) {
            log.warn("Failed to load product media", ex);
            return null;
        }
    }

    private String resolveStatusLabel(OrderStatus status) {
        if (status == null) {
            return "Không xác định";
        }
        return switch (status) {
            case PENDING_PAYMENT -> "Chờ thanh toán";
            case PLACED -> "Đã đặt hàng";
            case CONFIRMED -> "Đã xác nhận";
            case PACKED -> "Đã đóng gói";
            case SHIPPING -> "Đang giao";
            case DELIVERED -> "Đã giao";
            case CANCELLED -> "Đã hủy";
            case FAILED -> "Thất bại";
            case PENDING -> "Đang xử lý";
        };
    }

    private String resolveStatusClass(OrderStatus status, PaymentStatus paymentStatus) {
        if (paymentStatus == PaymentStatus.PAID) {
            return "is-paid";
        }
        if (status == null) {
            return "is-pending";
        }
        return switch (status) {
            case SHIPPING -> "is-shipped";
            case DELIVERED -> "is-completed";
            case CANCELLED, FAILED -> "is-cancelled";
            default -> "is-pending";
        };
    }

    private String resolvePaymentLabel(PaymentStatus paymentStatus) {
        if (paymentStatus == null) {
            return "Không xác định";
        }
        return switch (paymentStatus) {
            case PAID -> "Đã thanh toán";
            case FAILED -> "Thanh toán thất bại";
            case REFUNDED -> "Đã hoàn tiền";
            case UNPAID -> "Chưa thanh toán";
        };
    }

    private String formatCurrency(long value) {
        return NumberFormat.getCurrencyInstance(VI_VN).format(value);
    }

    private String nvl(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }

    private String textOrNull(JsonNode node, String field) {
        if (node == null || field == null) {
            return null;
        }
        JsonNode child = node.get(field);
        if (child == null || child.isNull()) {
            return null;
        }
        String text = child.asText();
        return text != null && !text.isBlank() ? text.trim() : null;
    }

    private String joinAddressParts(String... parts) {
        if (parts == null || parts.length == 0) {
            return null;
        }
        return java.util.Arrays.stream(parts)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .reduce((a, b) -> a + ", " + b)
                .orElse(null);
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null) {
                String trimmed = value.trim();
                if (!trimmed.isEmpty()) {
                    return trimmed;
                }
            }
        }
        return null;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

