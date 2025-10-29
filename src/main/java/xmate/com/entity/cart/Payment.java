package xmate.com.entity.cart;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import xmate.com.entity.sales.Order;

import java.time.Instant;

@Entity
@Getter
@Setter
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Mối quan hệ 1 Payment - 1 Order (một đơn hàng chỉ có một giao dịch)
    @ManyToOne(fetch = FetchType.LAZY)
    private Order order;

    /**
     * COD | VNPAY | MOMO | ZALOPAY
     */
    private String method;

    /**
     * Số tiền thanh toán (VND)
     */
    private long amount;

    /**
     * UNPAID | PENDING | PAID | FAILED | SUCCESS
     */
    private String status;

    /**
     * Mã giao dịch từ VNPay (vnp_TxnRef)
     */
    private String txnRef;

    /**
     * Chuỗi JSON hoặc map response từ VNPay IPN/return (để debug, verify)
     */
    @Lob
    private String raw;

    /**
     * Thời điểm tạo giao dịch
     */
    private Instant createdAt = Instant.now();

    /**
     * Thời điểm cập nhật trạng thái (sau khi IPN/Return)
     */
    private Instant updatedAt;

    public void setRaw(String rawData) {
        this.raw = rawData;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }

    @PrePersist
    public void onCreate() {
        this.createdAt = Instant.now();
    }
}
