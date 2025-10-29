package xmate.com.dto.checkout;


// DTO này chỉ dùng để hiển thị coupon trên giao diện
public record CouponDto(
        String code,
        String title,
        String expiryText // Ngày hết hạn đã được định dạng sẵn thành String
) {}