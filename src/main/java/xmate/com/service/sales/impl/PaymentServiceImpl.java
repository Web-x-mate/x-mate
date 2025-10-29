package xmate.com.service.sales.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import xmate.com.entity.enums.OrderStatus;
import xmate.com.entity.enums.PaymentStatus;
import xmate.com.entity.sales.Order;
import xmate.com.entity.sales.Payment;
import xmate.com.repo.sales.OrderRepository;
import xmate.com.repo.sales.SalePaymentRepository;
import xmate.com.service.sales.PaymentService;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final OrderRepository orderRepo;
    private final SalePaymentRepository paymentRepo;

    @Value("${app.upload.dir:uploads/proofs}")
    private String uploadDir;

    @Override
    @Transactional
    public Payment uploadProof(String orderCode, MultipartFile file, String note) throws IOException {
        Order order = orderRepo.findByCode(orderCode)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng: " + orderCode));

        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            throw new IllegalStateException("Đơn đã được thanh toán");
        }

        Payment payment = ensurePaymentRecord(order);

        // Lưu ảnh
        Path dir = Paths.get(uploadDir);
        Files.createDirectories(dir);

        String original = file.getOriginalFilename();
        String ext = (original != null && original.contains(".")) ? original.substring(original.lastIndexOf(".")) : ".jpg";
        String safeName = orderCode + "-" + System.currentTimeMillis() + ext;
        Path saved = dir.resolve(safeName);
        Files.copy(file.getInputStream(), saved, StandardCopyOption.REPLACE_EXISTING);

        payment.setProofImage("/uploads/proofs/" + safeName); // đường dẫn public
        payment.setProofNote(StringUtils.hasText(note) ? note : null);
        payment.setProofStatus(Payment.ProofStatus.SUBMITTED);
        payment.setSubmittedAt(LocalDateTime.now());

        return paymentRepo.save(payment);
    }

    @Override
    @Transactional
    public void approveProof(Long paymentId, String reviewer) {
        Payment p = paymentRepo.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy payment id=" + paymentId));

        p.setProofStatus(Payment.ProofStatus.APPROVED);
        p.setReviewedAt(LocalDateTime.now());
        p.setReviewedBy(reviewer);

        Order o = p.getOrder();
        o.setPaymentStatus(PaymentStatus.PAID);

        orderRepo.save(o);
        paymentRepo.save(p);
    }

    @Override
    @Transactional
    public void rejectProof(Long paymentId, String reviewer, String rejectNote) {
        Payment p = paymentRepo.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy payment id=" + paymentId));

        p.setProofStatus(Payment.ProofStatus.REJECTED);
        p.setReviewedAt(LocalDateTime.now());
        p.setReviewedBy(reviewer);
        if (StringUtils.hasText(rejectNote)) {
            p.setProofNote(rejectNote);
        }
        paymentRepo.save(p);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Payment> listPendingProofs() {
        return paymentRepo.findByProofStatus(Payment.ProofStatus.SUBMITTED);
    }

    @Override
    @Transactional(readOnly = true)
    public Payment getByOrderCode(String orderCode) {
        return paymentRepo.findByOrder_Code(orderCode).orElse(null);
    }

    @Override
    @Transactional
    public Payment ensurePaymentRecord(Order order) {
        Optional<Payment> ex = paymentRepo.findByOrder(order);
        if (ex.isPresent()) return ex.get();

        Payment p = new Payment();
        p.setOrder(order);
        p.setAmount(order.getTotal());
        // proofStatus sẽ set khi upload
        return paymentRepo.save(p);
    }
}
