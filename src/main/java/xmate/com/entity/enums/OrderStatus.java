package xmate.com.entity.enums;

public enum OrderStatus {
    PENDING_PAYMENT, // Chờ thanh toán (cho các đơn hàng online)
    PLACED,          // Mới đặt hàng (cho đơn COD)
    CONFIRMED,       // Đã xác nhận
    PACKED,          // Đã đóng gói
    SHIPPING,        // Đang giao hàng
    DELIVERED,       // Đã giao thành công
    CANCELLED,       // Đã hủy
    FAILED           // Thất bại
}