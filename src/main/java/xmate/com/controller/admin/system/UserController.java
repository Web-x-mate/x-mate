// src/main/java/xmate/com/controller/admin/system/UserAdminController.java
package xmate.com.controller.admin.system;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import xmate.com.entity.system.Role;
import xmate.com.entity.system.User;
import xmate.com.service.system.RoleService;
import xmate.com.service.system.UserService;

import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/system/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final RoleService roleService;

    @GetMapping
    public String index(@RequestParam(defaultValue = "") String q,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size,
                        Model model) {
        Page<User> p = userService.search(q, PageRequest.of(page, size, Sort.by("id").descending()));
        model.addAttribute("page", p);
        model.addAttribute("q", q);
        return "system/users/list";
    }
    @PreAuthorize("hasAnyAuthority('USER_CREATE','ROLE_ADMIN')")
    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("user", new User());
        model.addAttribute("allRoles", roleService.search("", PageRequest.of(0, 1000)).getContent());
        return "system/users/form";
    }
    @PreAuthorize("hasAnyAuthority('USER_CREATE','ROLE_ADMIN')")
    @PostMapping("/new")
    public String create(@ModelAttribute("user") User user,
                         @RequestParam(required = false, name = "roleIds") Set<Long> roleIds,
                         RedirectAttributes ra) {
        User saved = userService.create(user);
        if (roleIds != null && !roleIds.isEmpty()) {
            userService.assignRoles(saved.getId(), roleIds);
        }
        ra.addFlashAttribute("success", "Tạo user thành công");
        return "redirect:/admin/system/users";
    }
    @PreAuthorize("hasAnyAuthority('USER_EDIT','ROLE_ADMIN')")
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        User u = userService.get(id);
        model.addAttribute("user", u);
        model.addAttribute("allRoles", roleService.search("", PageRequest.of(0, 1000)).getContent());
        model.addAttribute("currentRoleIds",
                u.getRoles() != null ? u.getRoles().stream().map(Role::getId).collect(Collectors.toSet()) : Set.of());
        return "system/users/form";
    }
    @PreAuthorize("hasAnyAuthority('USER_EDIT','ROLE_ADMIN')")
    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @ModelAttribute("user") User user,
                         @RequestParam(required = false, name = "roleIds") Set<Long> roleIds,
                         RedirectAttributes ra) {
        userService.update(id, user);
        userService.assignRoles(id, roleIds != null ? roleIds : Set.of());
        ra.addFlashAttribute("success", "Cập nhật user thành công");
        return "redirect:/admin/system/users";
    }
    @PreAuthorize("hasAnyAuthority('USER_DELETE','ROLE_ADMIN')")
    @GetMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        userService.delete(id);
        ra.addFlashAttribute("success", "Đã xóa user");
        return "redirect:/admin/system/users";
    }
    @PreAuthorize("hasAnyAuthority('USER_STATUS_UPDATE','ROLE_ADMIN')")
    @PostMapping("/{id}/toggle")
    public String toggleActive(@PathVariable Long id, @RequestParam boolean active, RedirectAttributes ra) {
        userService.setActive(id, active);
        ra.addFlashAttribute("success", "Đã cập nhật trạng thái");
        return "redirect:/admin/system/users";
    }
}
