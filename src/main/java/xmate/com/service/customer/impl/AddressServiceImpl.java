// src/main/java/service/impl/AddressServiceImpl.java
package xmate.com.service.customer.impl;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import xmate.com.dto.address.CheckoutAddressVM;
import xmate.com.dto.address.AddressDto;
import xmate.com.entity.customer.Customer;
import xmate.com.repo.customer.AddressRepository;
import xmate.com.repo.customer.CustomerRepository;
import xmate.com.security.SecurityUtils;
import xmate.com.service.customer.AddressService;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepo;
    private final CustomerRepository customerRepo;

    public AddressServiceImpl(AddressRepository addressRepo, CustomerRepository customerRepo) {
        this.addressRepo = addressRepo;
        this.customerRepo = customerRepo;
    }

    @Override
    public List<AddressDto> listMine() {
        Long uid = currentUserId();
        return addressRepo.findByCustomerId(uid).stream()
                .map(a -> new AddressDto(
                        a.getId(),
                        null,
                        a.getPhone(),
                        a.getLine1(),
                        a.getWard(),
                        a.getDistrict(),
                        a.getCity(),
                        false
                ))
                .collect(Collectors.toList());
    }

    @Override
    public Object listForCurrentUser() {
        Long uid = currentUserId();
        return addressRepo.findByCustomerId(uid).stream()
                .map(a -> new CheckoutAddressVM(
                        a.getId(),
                        buildFull(a.getLine1(), a.getWard(), a.getDistrict(), a.getCity()),
                        a.getPhone(), a.getLine1(), a.getWard(), a.getDistrict(), a.getCity()
                ))
                .collect(Collectors.toList());
    }

    private Long currentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        String email = SecurityUtils.resolveEmail(auth);
        if (email == null || email.isBlank()) throw new IllegalStateException("Bạn chưa đăng nhập");
        return customerRepo.findByEmailIgnoreCase(email)
                .map(Customer::getId)
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy user: " + email));
    }

    private static String buildFull(String line1, String ward, String district, String city) {
        StringBuilder sb = new StringBuilder();
        if (line1 != null && !line1.isBlank()) sb.append(line1.trim());
        if (ward != null && !ward.isBlank()) sb.append(", ").append(ward.trim());
        if (district != null && !district.isBlank()) sb.append(", ").append(district.trim());
        if (city != null && !city.isBlank()) sb.append(", ").append(city.trim());
        return sb.toString();
    }
}
