// src/main/java/xmate/com/controller/admin/catalog/ProductMediaAdminController.java
package xmate.com.controller.admin.catalog;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
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

    @PreAuthorize("hasAnyAuthority('PRODUCT_MEDIA_CREATE','ROLE_ADMIN')")
    @GetMapping("/new")
    public String createForm(@PathVariable Long productId, Model model) {
        Product product = productService.get(productId);
        model.addAttribute("product", product);
        model.addAttribute("media", new ProductMedia());
        // dropdown variants nếu form cần
        model.addAttribute("variants", variantService.findByProductId(productId));
        return "catalog/products/media-form";
    }

    @PreAuthorize("hasAnyAuthority('PRODUCT_MEDIA_CREATE','ROLE_ADMIN')")
    @PostMapping("/new")
    public String create(@PathVariable Long productId,
                         @RequestParam("mediaType") String mediaType,
                         @RequestParam(name = "primary", defaultValue = "false") boolean primary,
                         @RequestParam(name = "sortOrder", defaultValue = "0") int sortOrder,
                         @RequestParam(name = "variantId", required = false) Long variantId,
                         @RequestParam(name = "file", required = false) MultipartFile file,
                         @RequestParam(name = "imageUrl", required = false) String imageUrl, // NEW
                         RedirectAttributes ra) {

        Product product = productService.get(productId);

        String url = null;
        if ("URL".equalsIgnoreCase(mediaType)) {
            // không cần file, chỉ cần link
            if (imageUrl == null || imageUrl.isBlank()) {
                ra.addFlashAttribute("error", "Vui lòng nhập link ảnh (URL).");
                return "redirect:/admin/catalog/products/" + productId + "/media/new";
            }
            url = imageUrl.trim();
        } else { // IMAGE
            if (file == null || file.isEmpty()) {
                ra.addFlashAttribute("error", "Vui lòng chọn file ảnh để tải lên.");
                return "redirect:/admin/catalog/products/" + productId + "/media/new";
            }
            url = storage.save(file); // lưu file -> URL public
        }

        ProductMedia m = new ProductMedia();
        m.setProduct(product);
        m.setUrl(url);
        m.setPrimary(primary);
        m.setSortOrder(sortOrder);
        m.setMediaType(xmate.com.entity.common.MediaType.valueOf(mediaType.toUpperCase()));
        if (variantId != null) {
            ProductVariant v = variantService.get(variantId);
            m.setVariant(v);
        }
        mediaService.create(m);

        ra.addFlashAttribute("success", "Đã thêm media");
        return "redirect:/admin/catalog/products/" + productId + "/media";
    }

    @PreAuthorize("hasAnyAuthority('PRODUCT_MEDIA_EDIT','ROLE_ADMIN')")
    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long productId, @PathVariable Long id, Model model) {
        Product product = productService.get(productId);
        ProductMedia media = mediaService.get(id);

        model.addAttribute("product", product);
        model.addAttribute("media", media);
        // dropdown variants nếu form cần
        model.addAttribute("variants", variantService.findByProductId(productId));

        return "catalog/products/media-form";
    }

    @PreAuthorize("hasAnyAuthority('PRODUCT_MEDIA_EDIT','ROLE_ADMIN')")
    @PostMapping("/edit/{id}")
    public String update(@PathVariable Long productId, @PathVariable Long id,
                         @RequestParam("mediaType") String mediaType,
                         @RequestParam(name = "primary", defaultValue = "false") boolean primary,
                         @RequestParam(name = "sortOrder", defaultValue = "0") int sortOrder,
                         @RequestParam(name = "variantId", required = false) Long variantId,
                         @RequestParam(name = "file", required = false) MultipartFile file,
                         @RequestParam(name = "imageUrl", required = false) String imageUrl, // NEW
                         RedirectAttributes ra) {

        ProductMedia m = mediaService.get(id);

        m.setMediaType(xmate.com.entity.common.MediaType.valueOf(mediaType.toUpperCase()));
        m.setPrimary(primary);
        m.setSortOrder(sortOrder);

        if (variantId != null) {
            m.setVariant(variantService.get(variantId));
        } else {
            m.setVariant(null);
        }

        if ("URL".equalsIgnoreCase(mediaType)) {
            // chuyển/giữ sang URL: nếu có URL mới thì set; nếu rỗng, giữ nguyên URL cũ
            if (imageUrl != null && !imageUrl.isBlank()) {
                // optional: nếu trước đó là file và bạn muốn dọn file cũ khi đổi sang URL mới
                if (m.getUrl() != null && (file == null || file.isEmpty())) {
                    // chỉ xóa nếu URL cũ khác URL mới để tránh xóa nhầm
                    if (!imageUrl.trim().equals(m.getUrl())) {
                        try { storage.deleteByUrl(m.getUrl()); } catch (Exception ignored) {}
                    }
                }
                m.setUrl(imageUrl.trim());
            }
            // không đụng tới file khi mediaType=URL
        } else { // IMAGE
            // nếu có file mới → replace
            if (file != null && !file.isEmpty()) {
                // xóa file cũ (optional)
                if (m.getUrl() != null) {
                    try { storage.deleteByUrl(m.getUrl()); } catch (Exception ignored) {}
                }
                String newUrl = storage.save(file);
                m.setUrl(newUrl);
            }
            // nếu không upload file mới thì giữ nguyên URL cũ
        }

        mediaService.update(id, m);
        ra.addFlashAttribute("success", "Đã cập nhật media");
        return "redirect:/admin/catalog/products/" + productId + "/media";
    }

    @PreAuthorize("hasAnyAuthority('PRODUCT_MEDIA_DELETE','ROLE_ADMIN')")
    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Long productId, @PathVariable Long id, RedirectAttributes ra) {
        ProductMedia m = mediaService.get(id);
        // xóa file thật (optional) — nên chỉ xóa nếu bạn đang lưu file nội bộ
        try { storage.deleteByUrl(m.getUrl()); } catch (Exception ignored) {}
        mediaService.delete(id);
        ra.addFlashAttribute("success", "Đã xóa media");
        return "redirect:/admin/catalog/products/" + productId + "/media";
    }
}
