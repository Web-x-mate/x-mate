// src/main/java/xmate/com/controller/admin/system/RoleAdminController.java
package xmate.com.controller.admin.system;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import xmate.com.domain.system.Permission;
import xmate.com.domain.system.Role;
import xmate.com.service.system.PermissionService;
import xmate.com.service.system.RoleService;

import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/system/roles")
@RequiredArgsConstructor
public class RoleController {
    private final RoleService roleService;
    private final PermissionService permService;

    @GetMapping
    public String index(@RequestParam(defaultValue = "") String q,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size,
                        Model model) {
        Page<Role> p = roleService.search(q, PageRequest.of(page, size, Sort.by("id").descending()));
        model.addAttribute("page", p);
        model.addAttribute("q", q);
        return "system/roles/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("role", new Role());
        model.addAttribute("allPerms", permService.search("", PageRequest.of(0, 2000)).getContent());
        return "system/roles/form";
    }

    @PostMapping("/new")
    public String create(@ModelAttribute("role") Role role,
                         @RequestParam(required = false, name = "permIds") Set<Long> permIds,
                         RedirectAttributes ra) {
        Role saved = roleService.create(role);
        if (permIds != null && !permIds.isEmpty()) {
            roleService.assignPermissions(saved.getId(), permIds);
        }
        ra.addFlashAttribute("success", "Tạo role thành công");
        return "redirect:/admin/system/roles";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Role r = roleService.get(id);
        model.addAttribute("role", r);
        model.addAttribute("allPerms", permService.search("", PageRequest.of(0, 2000)).getContent());
        model.addAttribute("currentPermIds",
                r.getPermissions() != null ? r.getPermissions().stream().map(Permission::getId).collect(Collectors.toSet()) : Set.of());
        return "system/roles/form";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @ModelAttribute("role") Role role,
                         @RequestParam(required = false, name = "permIds") Set<Long> permIds,
                         RedirectAttributes ra) {
        roleService.update(id, role);
        roleService.assignPermissions(id, permIds != null ? permIds : Set.of());
        ra.addFlashAttribute("success", "Cập nhật role thành công");
        return "redirect:/admin/system/roles";
    }

    @GetMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        roleService.delete(id);
        ra.addFlashAttribute("success", "Đã xóa role");
        return "redirect:/admin/system/roles";
    }
}
