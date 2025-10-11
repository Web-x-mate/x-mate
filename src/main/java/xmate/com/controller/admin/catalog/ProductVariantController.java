// src/main/java/xmate/com/controller/admin/catalog/ProductVariantAdminController.java
package xmate.com.controller.admin.catalog;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import xmate.com.entity.catalog.Product;
import xmate.com.entity.catalog.ProductVariant;
import xmate.com.entity.common.StockPolicy;
import xmate.com.service.catalog.ProductService;
import xmate.com.service.catalog.ProductVariantService;

import java.math.BigDecimal;

@Controller
@RequestMapping("/admin/catalog/products/{productId}/variants")
@RequiredArgsConstructor
public class ProductVariantController {

    private final ProductService productService;
    private final ProductVariantService variantService;

    @GetMapping
    public String list(@PathVariable Long productId,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "10") int size,
                       Model model) {
        Product product = productService.get(productId);
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<ProductVariant> variants = variantService.byProduct(productId, pageable);

        model.addAttribute("product", product);
        model.addAttribute("variants", variants);
        return "catalog/products/variants";
    }

    @GetMapping("/new")
    public String createForm(@PathVariable Long productId, Model model) {
        model.addAttribute("product", productService.get(productId));
        model.addAttribute("variant", new ProductVariant());
        model.addAttribute("stockPolicies", StockPolicy.values());
        return "catalog/products/variant-form";
    }

    @PostMapping("/new")
    public String create(@PathVariable Long productId,
                         @RequestParam String sku,
                         @RequestParam(required = false) String color,
                         @RequestParam(required = false) String size,
                         @RequestParam BigDecimal price,
                         @RequestParam(required = false) BigDecimal compareAtPrice,
                         @RequestParam(required = false) BigDecimal cost,
                         @RequestParam(required = false) Integer weightGram,
                         @RequestParam(defaultValue = "DENY") String stockPolicy,
                         @RequestParam(defaultValue = "true") boolean active,
                         @RequestParam(required = false) String barcode,
                         RedirectAttributes ra) {

        ProductVariant v = new ProductVariant();
        v.setProduct(productService.get(productId));
        v.setSku(sku);
        v.setColor(color);
        v.setSize(size);
        v.setPrice(price);
        v.setCompareAtPrice(compareAtPrice);
        v.setCost(cost);
        v.setWeightGram(weightGram);
        v.setStockPolicy(StockPolicy.valueOf(stockPolicy));
        v.setActive(active);
        v.setBarcode(barcode);

        variantService.create(v);
        ra.addFlashAttribute("success", "Đã thêm variant");
        return "redirect:/admin/catalog/products/" + productId + "/variants";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long productId, @PathVariable Long id, Model model) {
        model.addAttribute("product", productService.get(productId));
        model.addAttribute("variant", variantService.get(id));
        model.addAttribute("stockPolicies", StockPolicy.values());
        return "catalog/products/variant-form";
    }

    @PostMapping("/edit/{id}")
    public String update(@PathVariable Long productId, @PathVariable Long id,
                         @RequestParam String sku,
                         @RequestParam(required = false) String color,
                         @RequestParam(required = false) String size,
                         @RequestParam BigDecimal price,
                         @RequestParam(required = false) BigDecimal compareAtPrice,
                         @RequestParam(required = false) BigDecimal cost,
                         @RequestParam(required = false) Integer weightGram,
                         @RequestParam(defaultValue = "DENY") String stockPolicy,
                         @RequestParam(defaultValue = "true") boolean active,
                         @RequestParam(required = false) String barcode,
                         RedirectAttributes ra) {

        ProductVariant v = variantService.get(id);
        v.setSku(sku);
        v.setColor(color);
        v.setSize(size);
        v.setPrice(price);
        v.setCompareAtPrice(compareAtPrice);
        v.setCost(cost);
        v.setWeightGram(weightGram);
        v.setStockPolicy(StockPolicy.valueOf(stockPolicy));
        v.setActive(active);
        v.setBarcode(barcode);

        variantService.update(id, v);
        ra.addFlashAttribute("success", "Đã cập nhật variant");
        return "redirect:/admin/catalog/products/" + productId + "/variants";
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Long productId, @PathVariable Long id, RedirectAttributes ra) {
        variantService.delete(id);
        ra.addFlashAttribute("success", "Đã xóa variant");
        return "redirect:/admin/catalog/products/" + productId + "/variants";
    }
}
