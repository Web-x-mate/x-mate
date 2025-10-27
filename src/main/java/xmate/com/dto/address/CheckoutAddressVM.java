package xmate.com.dto.address;

public class CheckoutAddressVM {
    private Long id;
    private String fullAddress;
    private String phone;
    private String line1;
    private String ward;
    private String district;
    private String city;

    public CheckoutAddressVM(Long id, String fullAddress) {
        this.id = id; this.fullAddress = fullAddress;
    }

    public CheckoutAddressVM(Long id, String fullAddress, String phone,
                             String line1, String ward, String district, String city) {
        this.id = id; this.fullAddress = fullAddress; this.phone = phone;
        this.line1 = line1; this.ward = ward; this.district = district; this.city = city;
    }

    public Long getId() { return id; }
    public String getFullAddress() { return fullAddress; }
    public String getPhone() { return phone; }
    public String getLine1() { return line1; }
    public String getWard() { return ward; }
    public String getDistrict() { return district; }
    public String getCity() { return city; }
}
