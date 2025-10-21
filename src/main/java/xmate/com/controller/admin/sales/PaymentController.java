package xmate.com.controller.admin.sales;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import xmate.com.service.sales.PaymentService;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;

    @PostMapping("/proof")
    public ResponseEntity<String> uploadProof(
            @RequestParam String orderCode,
            @RequestParam MultipartFile file,
            @RequestParam(required = false) String note) throws Exception {
        paymentService.uploadProof(orderCode, file, note);
        return ResponseEntity.ok("Đã gửi biên lai. Vui lòng chờ xác nhận!");
    }
}
