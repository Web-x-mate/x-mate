package xmate.com.repo.customer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import xmate.com.entity.customer.CustomerMembership;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CustomerMembershipRepo extends JpaRepository<CustomerMembership, Long> {
    // Lấy 1 membership theo customerId (userId = customer.id)
    Optional<CustomerMembership> findByUserId(Long userId);

    // Lấy theo danh sách customerId — dùng cho trang chờ (tránh N+1)
    List<CustomerMembership> findByUserIdIn(Collection<Long> userIds);

    // (Tuỳ chọn) Nếu muốn viết tường minh bằng JPQL:
    @Query("select m from CustomerMembership m where m.userId in :ids")
    List<CustomerMembership> findAllByUserIds(@Param("ids") Collection<Long> ids);
}
