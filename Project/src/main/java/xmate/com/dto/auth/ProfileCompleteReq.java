package xmate.com.dto.auth;

// DTO
public record ProfileCompleteReq(
        String dob,           // "yyyy-MM-dd" hoặc null
        String gender,        // "M"/"F"/"O" hoặc null
        Integer heightCm,     // hoặc null
        Integer weightKg,     // hoặc null
        String phone          // có thể null; bắt buộc nếu user chưa có số
) {}
