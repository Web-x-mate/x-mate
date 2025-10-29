//package xmate.com.controller.web;
//
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Controller;
//import org.springframework.ui.Model;
//import org.springframework.web.bind.annotation.GetMapping;
//import xmate.com.entity.customer.Customer;
//import xmate.com.repo.customer.CustomerRepository;
//import xmate.com.util.SecurityUtil;
//
//@Controller
//@RequiredArgsConstructor
//public class HomeController {
//
//    private final CustomerRepository userRepo;
//
//    @GetMapping("/")
//    public String home(Model model) {
//        String meEmail = SecurityUtil.currentEmail(); // null nếu chưa login
//        model.addAttribute("meEmail", meEmail);
//
//        boolean isAdmin = false;
//        if (meEmail != null) {
//            Customer u = userRepo.findByEmail(meEmail).orElse(null);
//
//        }
//        model.addAttribute("isAdmin", isAdmin);
//
//        String sampleUserEmail = userRepo.findAll().stream()
//
//                .map(Customer::getEmail)
//                .findFirst().orElse("user1@example.com");
//        model.addAttribute("sampleUserEmail", sampleUserEmail);
//
//        return "auth/login";
//    }
//}
