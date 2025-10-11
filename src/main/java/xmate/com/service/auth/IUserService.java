package xmate.com.service.auth;

import java.util.List;

import xmate.com.dto.auth.AddressReq;
import xmate.com.dto.auth.MeRes;
import xmate.com.dto.auth.UpdateMeReq;
import xmate.com.entity.customer.Address;

public interface IUserService {
    MeRes getMe(String email);
    MeRes updateMe(String email, UpdateMeReq req);
    List<Address> getMyAddresses(String email);
    Address addAddress(String email, AddressReq req);

    void upsertGoogleUser(String email, String fullName);

    void updatePhone(String email, String phone);
}
