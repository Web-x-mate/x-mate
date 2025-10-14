// src/main/java/xmate/com/service/system/UserProfileService.java
package xmate.com.service.system;

import xmate.com.entity.system.User;

public interface UserProfileService {

    /** Lấy hồ sơ theo username đang đăng nhập */
    User getProfileOf(String username);

    /** User tự cập nhật một số trường (fullName, phone, email) */
    User updateSelf(String username, String fullName, String phone, String email);
}
