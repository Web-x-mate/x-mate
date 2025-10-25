package xmate.com.controller.client.view;

import java.util.List;

public record ProductCardView(
        Long id,
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
        List<ProductColorView> colors
) {
    @Override
    public String toString() {
        var colorNames = colors == null ? "" :
                colors.stream().map(ProductColorView::name).reduce("", (acc, name) -> acc.isEmpty() ? name : acc + "," + name);
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
