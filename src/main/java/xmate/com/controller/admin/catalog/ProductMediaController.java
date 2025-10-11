// src/main/java/xmate/com/controller/admin/catalog/ProductMediaAdminController.java
package xmate.com.controller.admin.catalog;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import xmate.com.entity.catalog.Product;
import xmate.com.entity.catalog.ProductMedia;
import xmate.com.entity.catalog.ProductVariant;
import xmate.com.service.catalog.ProductMediaService;
import xmate.com.service.catalog.ProductService;
import xmate.com.service.catalog.ProductVariantService;
import xmate.com.storage.FileStorageService;

import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@Controller
@RequestMapping("/admin/catalog/products/{productId}/media")
@RequiredArgsConstructor
public class ProductMediaController {

    private final ProductService productService;
    private final ProductMediaService mediaService;
    private final ProductVariantService variantService;
    private final FileStorageService storage;

    @GetMapping
    public String list(@PathVariable Long productId,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "10") int size,
                       Model model) {
        Product product = productService.get(productId);
        List<ProductMedia> all = mediaService.forProduct(productId); // đã sort
        Pageable pageable = PageRequest.of(page, size);
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), all.size());
        Page<ProductMedia> mediaPage = new PageImpl<>(
                start <= end ? all.subList(start, end) : List.of(),
                pageable, all.size());

        model.addAttribute("product", product);
        model.addAttribute("mediaPage", mediaPage);
        return "catalog/products/media";
    }

    @GetMapping("/new")
    public String createForm(@PathVariable Long productId, Model model) {
        model.addAttribute("product", productService.get(productId));
        model.addAttribute("media", new ProductMedia());
        return "catalog/products/media-form";
    }

    @PostMapping("/new")
    public String create(@PathVariable Long productId,
                         @RequestParam("mediaType") String mediaType,
                         @RequestParam(name = "primary", defaultValue = "false") boolean primary,
                         @RequestParam(name = "sortOrder", defaultValue = "0") int sortOrder,
                         @RequestParam(name = "variantId", required = false) Long variantId,
                         @RequestParam(name = "file") MultipartFile file,
                         RedirectAttributes ra) {

        Product product = productService.get(productId);
        String url = storage.save(file); // lưu file -> URL public

        ProductMedia m = new ProductMedia();
        m.setProduct(product);
        m.setUrl(url);
        m.setPrimary(primary);
        m.setSortOrder(sortOrder);
        m.setMediaType(xmate.com.entity.common.MediaType.valueOf(mediaType));
        if (variantId != null) {
            ProductVariant v = variantService.get(variantId);
            m.setVariant(v);
        }
        mediaService.create(m);

        ra.addFlashAttribute("success", "Đã thêm media");
        return "redirect:/admin/catalog/products/" + productId + "/media";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long productId, @PathVariable Long id, Model model) {
        model.addAttribute("product", productService.get(productId));
        model.addAttribute("media", mediaService.get(id));
        return "catalog/products/media-form";
    }

    @PostMapping("/edit/{id}")
    public String update(@PathVariable Long productId, @PathVariable Long id,
                         @RequestParam("mediaType") String mediaType,
                         @RequestParam(name = "primary", defaultValue = "false") boolean primary,
                         @RequestParam(name = "sortOrder", defaultValue = "0") int sortOrder,
                         @RequestParam(name = "variantId", required = false) Long variantId,
                         @RequestParam(name = "file", required = false) MultipartFile file,
                         RedirectAttributes ra) {

        ProductMedia m = mediaService.get(id);
        m.setMediaType(xmate.com.entity.common.MediaType.valueOf(mediaType));
        m.setPrimary(primary);
        m.setSortOrder(sortOrder);

        if (variantId != null) {
            m.setVariant(variantService.get(variantId));
        } else {
            m.setVariant(null);
        }

        // nếu chọn file mới → replace
        if (file != null && !file.isEmpty()) {
            // xóa file cũ (optional)
            storage.deleteByUrl(m.getUrl());
            String newUrl = storage.save(file);
            m.setUrl(newUrl);
        }

        mediaService.update(id, m);
        ra.addFlashAttribute("success", "Đã cập nhật media");
        return "redirect:/admin/catalog/products/" + productId + "/media";
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Long productId, @PathVariable Long id, RedirectAttributes ra) {
        ProductMedia m = mediaService.get(id);
        storage.deleteByUrl(m.getUrl()); // xóa file thật (optional)
        mediaService.delete(id);
        ra.addFlashAttribute("success", "Đã xóa media");
        return "redirect:/admin/catalog/products/" + productId + "/media";
    }
}
