package xmate.com.dto.address;
public record AddressDto(
        Long id,
        String fullname,
        String phone,
        String line1,
        String ward,
        String district,
        String province,
        boolean isDefault) {}
