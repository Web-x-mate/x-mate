package xmate.com.service.customer;
import xmate.com.dto.address.AddressDto;
import xmate.com.dto.address.AccountAddressForm;
import java.util.List;
public interface AddressService {
    List<AddressDto> listMine();
    List<AddressDto> listForCurrentUser();
    AddressDto save(AccountAddressForm form);

    void setDefault(Long addressId);
    void delete(Long addressId);
}
