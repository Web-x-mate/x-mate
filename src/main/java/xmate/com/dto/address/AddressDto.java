package xmate.com.dto.address;

public class AddressDto {

    private final Long id;
    private final String fullName;
    private final String phone;
    private final String line1;
    private final String line2;
    private final String city;
    private final String district;
    private final String ward;
    private final boolean defaultAddress;

    public AddressDto(
            Long id,
            String fullName,
            String phone,
            String line1,
            String line2,
            String city,
            String district,
            String ward,
            boolean defaultAddress
    ) {
        this.id = id;
        this.fullName = fullName;
        this.phone = phone;
        this.line1 = line1;
        this.line2 = line2;
        this.city = city;
        this.district = district;
        this.ward = ward;
        this.defaultAddress = defaultAddress;
    }

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getPhone() {
        return phone;
    }

    public String getLine1() {
        return line1;
    }

    public String getLine2() {
        return line2;
    }

    public String getCity() {
        return city;
    }

    public String getDistrict() {
        return district;
    }

    public String getWard() {
        return ward;
    }

    public boolean isDefaultAddress() {
        return defaultAddress;
    }

    public boolean isDefault() {
        return defaultAddress;
    }

    public boolean getDefault() {
        return defaultAddress;
    }
}
