// src/main/java/xmate/com/controller/admin/catalog/ProductAdminController.java
package xmate.com.controller.admin.catalog;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import xmate.com.entity.catalog.Product;
import xmate.com.entity.catalog.Category;
import xmate.com.service.catalog.ProductService;
import xmate.com.service.catalog.CategoryService;

@Controller
@RequestMapping("/admin/catalog/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final CategoryService categoryService;


    @GetMapping
    public String index(@RequestParam(defaultValue = "") String q,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size,
                        @RequestParam(required = false) Long categoryId,
                        Model model) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<Product> p = (categoryId != null)
                ? productService.byCategory(categoryId, pageable)
                : productService.search(q, pageable);

        model.addAttribute("page", p);
        model.addAttribute("q", q);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("categories", categoryService.list(PageRequest.of(0, Integer.MAX_VALUE)).getContent()); // dropdown lọc
        return "catalog/products/list"; // -> templates/catalog/products/list.html
    }

    @PreAuthorize("hasAuthority('PRODUCT_VIEW_DETAIL')")
    @GetMapping("/{id}")
    public String view(@PathVariable Long id, Model model) {
        model.addAttribute("product", productService.get(id));
        return "catalog/products/view"; // nếu chưa có có thể tạm dùng form
    }

    @PreAuthorize("hasAuthority('PRODUCT_CREATE')")
    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("product", new Product());
        model.addAttribute("categories", categoryService.list(PageRequest.of(0, Integer.MAX_VALUE)).getContent());
        return "catalog/products/form"; // -> templates/catalog/products/form.html
    }

    @PreAuthorize("hasAuthority('PRODUCT_CREATE')")
    @PostMapping("/new")
    public String create(@ModelAttribute Product product,
                         @RequestParam(required = false) Long categoryId,
                         RedirectAttributes ra) {
        if (categoryId != null) {
            Category cat = categoryService.get(categoryId);
            product.setCategory(cat);
        } else {
            product.setCategory(null);
        }
        productService.create(product);
        ra.addFlashAttribute("success", "Tạo sản phẩm thành công");
        return "redirect:/admin/catalog/products";
    }

    @PreAuthorize("hasAuthority('PRODUCT_EDIT')")
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("product", productService.get(id));
        model.addAttribute("categories", categoryService.list(PageRequest.of(0, Integer.MAX_VALUE)).getContent());
        return "catalog/products/form";
    }

    @PreAuthorize("hasAuthority('PRODUCT_EDIT')")
    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @ModelAttribute Product product,
                         @RequestParam(required = false) Long categoryId,
                         RedirectAttributes ra) {
        if (categoryId != null) {
            Category cat = categoryService.get(categoryId);
            product.setCategory(cat);
        } else {
            product.setCategory(null);
        }
        productService.update(id, product);
        ra.addFlashAttribute("success", "Cập nhật sản phẩm thành công");
        return "redirect:/admin/catalog/products";
    }

    @PreAuthorize("hasAuthority('PRODUCT_DELETE')")
    @GetMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        productService.delete(id);
        ra.addFlashAttribute("success", "Đã xóa sản phẩm");
        return "redirect:/admin/catalog/products";
    }
}
