package xmate.com.service.customer.impl;

import java.util.ArrayList;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import xmate.com.dto.address.AccountAddressForm;
import xmate.com.dto.address.AddressDto;
import xmate.com.entity.customer.Address;
import xmate.com.entity.customer.Customer;
import xmate.com.repo.customer.AddressRepository;
import xmate.com.repo.customer.CustomerRepository;
import xmate.com.service.customer.AddressService;

@Service
@RequiredArgsConstructor
@Transactional
public class AddressServiceImpl implements AddressService {

    private static final Logger log = LoggerFactory.getLogger(AddressServiceImpl.class);

    private final CustomerRepository customerRepository;
    private final AddressRepository addressRepository;

    @Override
    @Transactional(readOnly = true)
    public List<AddressDto> listMine() {
        return listForCurrentUser();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AddressDto> listForCurrentUser() {
        Customer me = currentCustomerOr401();
        return addressRepository.findByCustomerId(me.getId()).stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public AddressDto save(AccountAddressForm form) {
        Customer me = currentCustomerOr401();
        List<Address> existing = addressRepository.findByCustomerId(me.getId());
        log.info("AddressService.save userId={} existingCount={} form={}", me.getId(), existing.size(), form);

        Address target;
        if (form.getAddressId() != null) {
            target = existing.stream()
                    .filter(addr -> addr.getId().equals(form.getAddressId()))
                    .findFirst()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Address not found"));
            log.info("Updating address id={} for user {}", target.getId(), me.getId());
        } else {
            target = new Address();
            target.setCustomer(me);
            existing.add(target);
            log.info("Creating new address for user {}", me.getId());
        }

        // Map form -> entity (dùng getters của class)
        target.setFullName(form.getFullName());
        target.setPhone(form.getPhone());
        target.setLine1(form.getLine1());
        target.setLine2(form.getLine2());
        target.setCity(form.getCity());
        target.setDistrict(form.getDistrict());
        target.setWard(form.getWard());

        boolean shouldBeDefault = form.isDefaultAddress()
                || existing.stream().filter(addr -> addr.getId() != null).count() == 0;

        List<Address> dirty = new ArrayList<>();

        if (shouldBeDefault) {
            for (Address addr : existing) {
                if (addr == target) continue;
                if (Boolean.TRUE.equals(addr.getDefaultAddress())) {
                    addr.setDefaultAddress(Boolean.FALSE);
                    dirty.add(addr);
                }
            }
            target.setDefaultAddress(Boolean.TRUE);
        } else {
            if (Boolean.TRUE.equals(target.getDefaultAddress()) && !form.isDefaultAddress()) {
                target.setDefaultAddress(Boolean.FALSE);
            } else if (target.getDefaultAddress() == null) {
                target.setDefaultAddress(Boolean.FALSE);
            }
        }

        Address saved = addressRepository.save(target);
        if (!dirty.isEmpty()) {
            addressRepository.saveAll(dirty);
            log.info("Cleared default flag on {} other addresses for user {}", dirty.size(), me.getId());
        }
        log.info("AddressService.save success userId={} addressId={}", me.getId(), saved.getId());
        return toDto(saved);
    }

    private Customer currentCustomerOr401() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Bạn chưa đăng nhập.");
        }
        String principal = auth.getName();
        if (principal == null || "anonymousUser".equalsIgnoreCase(principal)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Bạn chưa đăng nhập.");
        }
        Customer result = customerRepository.findByEmailIgnoreCase(principal)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Không tìm thấy khách hàng."));
        log.debug("currentCustomerOr401 principal={} -> id={}", principal, result.getId());
        return result;
    }

    private AddressDto toDto(Address address) {
        return new AddressDto(
                address.getId(),
                address.getFullName(),
                address.getPhone(),
                address.getLine1(),
                address.getLine2(),
                address.getCity(),
                address.getDistrict(),
                address.getWard(),
                Boolean.TRUE.equals(address.getDefaultAddress())
        );
    }

    // ... trong AddressServiceImpl

@Override
public void setDefault(Long addressId) {
    Customer me = currentCustomerOr401();
    // bỏ mặc định các địa chỉ khác, đặt mặc định cho addressId
    Address address = addressRepository.findById(addressId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Address not found"));
    if (!address.getCustomer().getId().equals(me.getId())) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
    }
    // unset others
    List<Address> all = addressRepository.findByCustomerId(me.getId());
    for (Address a : all) {
        if (!a.getId().equals(addressId) && Boolean.TRUE.equals(a.getDefaultAddress())) {
            a.setDefaultAddress(Boolean.FALSE);
        }
    }
    address.setDefaultAddress(Boolean.TRUE);
    addressRepository.saveAll(all);
}

@Override
public void delete(Long addressId) {
    Customer me = currentCustomerOr401();
    Address address = addressRepository.findById(addressId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Address not found"));
    if (!address.getCustomer().getId().equals(me.getId())) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
    }
    if (Boolean.TRUE.equals(address.getDefaultAddress())) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể xóa địa chỉ mặc định.");
    }
    addressRepository.delete(address);
}

}
