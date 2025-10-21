package xmate.com.dto.checkout;

// Thêm các trường cho địa chỉ mới vào record này
public record CheckoutReq(
        Long addressId, // Sẽ là null nếu người dùng tạo địa chỉ mới
        String couponCode,
        String paymentMethod,
        String note,

        // Các trường cho địa chỉ mới
        String newAddressFullName,
        String newAddressPhone,
        String newAddressEmail,
        String newAddressLine1, // Địa chỉ chi tiết
        String newAddressCity
) {
    public String newAddressWard() {return  null;}

    public String newAddressDistrict() {    return  null;
    }
}
