package xmate.com.repo.procurement;

// Có thể đặt ngay trong file này nhưng ngoài scope repository
public interface StatusCount {
    String getLabel();
    Long getValue(); // COUNT(*) thường nên dùng Long
}
