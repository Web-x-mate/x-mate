// src/main/java/xmate/com/controller/admin/system/PermissionAdminController.java
package xmate.com.controller.admin.system;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import xmate.com.entity.system.Permission;
import xmate.com.service.system.PermissionService;

@Controller
@RequestMapping("/admin/system/permissions")
@RequiredArgsConstructor
public class PermissionController {
    private final PermissionService permService;

    @GetMapping
    public String index(@RequestParam(defaultValue = "") String q,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size,
                        Model model) {
        Page<Permission> p = permService.search(q, PageRequest.of(page, size, Sort.by("id").descending()));
        model.addAttribute("page", p);
        model.addAttribute("q", q);
        return "system/permissions/list";
    }
    @PreAuthorize("hasAnyAuthority('PERM_MGMT_CREATE','ROLE_ADMIN')")
    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("permission", new Permission());
        return "system/permissions/form";
    }
    @PreAuthorize("hasAnyAuthority('PERM_MGMT_CREATE','ROLE_ADMIN')")
    @PostMapping("/new")
    public String create(@ModelAttribute("permission") Permission permission, RedirectAttributes ra) {
        permService.create(permission);
        ra.addFlashAttribute("success", "Tạo permission thành công");
        return "redirect:/admin/system/permissions";
    }
    @PreAuthorize("hasAnyAuthority('PERM_MGMT_EDIT','ROLE_ADMIN')")
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("permission", permService.get(id));
        return "system/permissions/form";
    }
    @PreAuthorize("hasAnyAuthority('PERM_MGMT_EDIT','ROLE_ADMIN')")
    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id, @ModelAttribute("permission") Permission permission, RedirectAttributes ra) {
        permService.update(id, permission);
        ra.addFlashAttribute("success", "Cập nhật permission thành công");
        return "redirect:/admin/system/permissions";
    }
    @PreAuthorize("hasAnyAuthority('PERM_MGMT_DELETE','ROLE_ADMIN')")
    @GetMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        permService.delete(id);
        ra.addFlashAttribute("success", "Đã xóa permission");
        return "redirect:/admin/system/permissions";
    }
}
