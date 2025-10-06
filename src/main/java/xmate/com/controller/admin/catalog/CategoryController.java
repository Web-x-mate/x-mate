// src/main/java/xmate/com/controller/admin/catalog/CategoryAdminController.java
package xmate.com.controller.admin.catalog;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import xmate.com.domain.catalog.Category;
import xmate.com.service.catalog.CategoryService;

@Controller
@RequestMapping("/admin/catalog/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    // LIST
    @GetMapping
    public String index(@RequestParam(defaultValue = "") String q,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size,
                        Model model) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<Category> p = categoryService.search(q, pageable);
        model.addAttribute("page", p);
        model.addAttribute("q", q);
        return "catalog/categories/list"; // -> templates/catalog/categories/list.html
    }

    // VIEW (optional)
    @GetMapping("/{id}")
    public String view(@PathVariable Long id, Model model) {
        model.addAttribute("category", categoryService.get(id));
        return "catalog/categories/view"; // nếu chưa có có thể tạm dùng form
    }

    // NEW (GET form)
    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("category", new Category());
        return "catalog/categories/form"; // -> templates/catalog/categories/form.html
    }

    // NEW (POST submit)
    @PostMapping("/new")
    public String create(@ModelAttribute Category category,
                         @RequestParam(required = false) Long parentId,
                         @RequestParam(defaultValue = "false") boolean active,
                         RedirectAttributes ra) {
        if (parentId != null) {
            category.setParent(categoryService.get(parentId));
        } else category.setParent(null);
        category.setActive(active);
        categoryService.create(category);
        ra.addFlashAttribute("success", "Tạo category thành công");
        return "redirect:/admin/catalog/categories";
    }

    // EDIT (GET form)
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("category", categoryService.get(id));
        return "catalog/categories/form";
    }

    // EDIT (POST submit)
    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @ModelAttribute Category category,
                         @RequestParam(required = false) Long parentId,
                         @RequestParam(defaultValue = "false") boolean active,
                         RedirectAttributes ra) {
        if (parentId != null) {
            category.setParent(categoryService.get(parentId));
        } else category.setParent(null);
        category.setActive(active);
        categoryService.update(id, category);
        ra.addFlashAttribute("success", "Cập nhật category thành công");
        return "redirect:/admin/catalog/categories";
    }

    // DELETE (đơn giản dùng GET → redirect)
    @GetMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        categoryService.delete(id);
        ra.addFlashAttribute("success", "Đã xóa category");
        return "redirect:/admin/catalog/categories";
    }
}
