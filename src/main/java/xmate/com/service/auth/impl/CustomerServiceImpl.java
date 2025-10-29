package xmate.com.service.auth.impl;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xmate.com.dto.auth.AddressReq;
import xmate.com.dto.auth.MeRes;
import xmate.com.dto.auth.UpdateMeReq;
import xmate.com.entity.customer.Address;
import xmate.com.entity.customer.Customer;
import xmate.com.repo.customer.AddressRepository;
import xmate.com.repo.customer.CustomerRepository;
import xmate.com.service.auth.IUserService;

import java.util.List;
import java.util.UUID;

@Service
public class CustomerServiceImpl implements IUserService {

    private final CustomerRepository userRepo;
    private final AddressRepository addrRepo;
    private final PasswordEncoder passwordEncoder;

    public CustomerServiceImpl(CustomerRepository userRepo,
                               AddressRepository addrRepo,
                               PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.addrRepo = addrRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public MeRes getMe(String email) {
        var u = userRepo.findByEmailIgnoreCase(norm(email)).orElseThrow();
        return new MeRes(
                u.getId(),
                u.getEmail(),
                u.getFullname(),
                u.getPhone()
        );
    }

    @Override
    public MeRes updateMe(String email, UpdateMeReq req) {
        var u = userRepo.findByEmailIgnoreCase(norm(email)).orElseThrow();
        u.setFullname(req.fullname());
        u.setPhone(req.phone());
        userRepo.save(u);
        return getMe(email);
    }

    @Override
    public List<Address> getMyAddresses(String email) {
        var u = userRepo.findByEmailIgnoreCase(email).orElseThrow();
        return addrRepo.findByCustomerId(u.getId());
    }

    @Override
    public Address addAddress(String email, AddressReq req) {
        var u = userRepo.findByEmailIgnoreCase(email).orElseThrow();
        boolean makeDefault = addrRepo.findByCustomerId(u.getId()).isEmpty();
        var a = Address.builder()
                .customer(u)
                .fullName(u.getFullname())
                .line1(req.line1())
                .line2(req.line2())
                .ward(req.ward())
                .district(req.district())
                .city(req.city())
                .phone(req.phone())
                .defaultAddress(makeDefault)
                .build();
        return addrRepo.save(a);
    }

    @Override
    @Transactional
    public void upsertGoogleUser(String email, String fullName) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("OAuth2 email is null/blank");
        }
        String key = norm(email);
        var u = userRepo.findByEmailIgnoreCase(key).orElse(null);
        if (u == null) {
            // KHÔNG dùng role cho khách hàng
            u = new Customer();
            u.setEmail(key);
            u.setFullname(fullName);
            String rnd = UUID.randomUUID().toString();
            u.setPasswordHash(passwordEncoder.encode(rnd));
            u.setEnabled(true);
            userRepo.save(u);
        } else {
            // cập nhật nhẹ nếu thiếu tên
            if ((u.getFullname() == null || u.getFullname().isBlank()) && fullName != null) {
                u.setFullname(fullName);
            }
            userRepo.save(u);
        }
    }


    @Override
    @Transactional
    public void updatePhone(String email, String phone) {
        var u = userRepo.findByEmailIgnoreCase(email).orElseThrow();
        u.setPhone(phone);
        userRepo.save(u);
    }
    private static String norm(String s) {
        return s == null ? null : s.trim().toLowerCase();
    }
}
