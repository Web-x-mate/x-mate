package xmate.com.controller.client.view;

import java.math.BigDecimal;
import java.util.List;

public record ProductColorView(
        String name,
        String hex,
        String swatchUrl,
        String image,
        String hoverImage,
        Long variantId,
        String style,
        BigDecimal price,
        BigDecimal compareAt,
        List<String> sizes
) {
}
