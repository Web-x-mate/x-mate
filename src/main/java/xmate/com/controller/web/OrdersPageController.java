//package xmate.com.controller.web;
//
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Controller;
//import org.springframework.ui.Model;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PathVariable;
//import org.springframework.web.bind.annotation.RequestParam;
//import xmate.com.dto.orders.OrderDetailDto;
//import xmate.com.service.cart.OrderQueryService;
//import xmate.com.service.cart.VietQRService;
//
//@Controller
//@RequiredArgsConstructor
//public class OrdersPageController {
//    private final OrderQueryService orderQueryService;
//    private final VietQRService vietQRService;
//
//    @GetMapping("/orders")
//    public String history(@RequestParam(defaultValue="0") int page, Model m){
//        // Giả sử service trả về một Page hoặc List DTO tóm tắt đơn hàng
//        m.addAttribute("orders", orderQueryService.listByUser(page, 10));
//        return "order/history";
//    }
//
//    @GetMapping("/orders/{code}")
//    public String detail(@PathVariable String code, Model m){
//        m.addAttribute("order", orderQueryService.detail(code));
//        return "order/detail";
//    }
//
//    @GetMapping("/orders/pay/{code}")
//    public String paymentPage(@PathVariable String code, Model model) {
//        OrderDetailDto order = orderQueryService.detail(code);
//        if (order == null) {
//            return "redirect:/orders"; // Nếu không tìm thấy đơn hàng
//        }
//
//        try {
//            // Tạo URL VietQR ngay tại đây
//            String vietQrUrl = vietQRService.generateQRCodeUrl(
//                    "970436", // Vietcombank BIN
//                    "1040487299", // Số tài khoản của bạn
//                    "VO AN THAI", // Tên chủ tài khoản
//                    order.total(),
//                    "Thanh toan don hang " + order.code()
//            );
//
//            model.addAttribute("order", order);
//
//            // --- SỬA LỖI: Bỏ dấu gạch nối "-" ở đầu dòng ---
//            model.addAttribute("vietQrUrl", vietQrUrl);
//
//            return "order/payment-qr"; // Trả về file HTML mới
//        } catch (Exception e) {
//            // Xử lý nếu có lỗi tạo QR
//            e.printStackTrace(); // In lỗi ra console để debug
//            return "redirect:/orders/" + code;
//        }
//    }
//}