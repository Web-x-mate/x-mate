package xmate.com.service.sales;

import org.springframework.web.multipart.MultipartFile;
import xmate.com.entity.sales.Order;
import xmate.com.entity.sales.Payment;

import java.io.IOException;
import java.util.List;

public interface PaymentService {

    /** Khách upload biên lai cho orderCode */
    Payment uploadProof(String orderCode, MultipartFile file, String note) throws IOException;

    /** Admin duyệt biên lai -> cập nhật order = PAID */
    void approveProof(Long paymentId, String reviewer);

    /** Admin từ chối biên lai */
    void rejectProof(Long paymentId, String reviewer, String rejectNote);

    /** Danh sách biên lai đang chờ duyệt */
    List<Payment> listPendingProofs();

    /** Lấy payment theo orderCode (tiện cho trang checkout) */
    Payment getByOrderCode(String orderCode);

    /** Đảm bảo có bản ghi payment cho order (nếu chưa có sẽ tạo) */
    Payment ensurePaymentRecord(Order order);
}
