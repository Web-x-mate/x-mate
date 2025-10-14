// src/main/java/service/AddressService.java
package xmate.com.service.customer;
import xmate.com.dto.address.AddressDto;
import java.util.List;
public interface AddressService { List<AddressDto> listMine();

    Object listForCurrentUser();
}
