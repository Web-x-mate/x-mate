// src/main/java/service/impl/AddressServiceImpl.java
package xmate.com.service.customer.impl;

import xmate.com.dto.address.AddressDto;
import xmate.com.service.customer.AddressService;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class AddressServiceImpl implements AddressService {
    @Override public List<AddressDto> listMine(){ return List.of(); }

    @Override
    public Object listForCurrentUser() {
        return null;
    }
}
