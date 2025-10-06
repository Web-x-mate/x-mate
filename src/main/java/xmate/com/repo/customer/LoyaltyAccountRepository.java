// src/main/java/xmate/com/repo/customer/LoyaltyAccountRepository.java
package xmate.com.repo.customer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import xmate.com.domain.customer.LoyaltyAccount;

import java.util.List;

public interface LoyaltyAccountRepository extends JpaRepository<LoyaltyAccount, Long> {

    /**
     * Tổng điểm theo từng tier (dùng cho biểu đồ Loyalty).
     * Trả về projection (label,value) để map thẳng vào JSON.
     * Lưu ý: alias phải là "label" và "value" để Spring map đúng.
     */
    @Query(value = """
        SELECT COALESCE(la.tier, 'UNSET') AS label,
               COALESCE(SUM(la.points), 0) AS value
        FROM loyalty_accounts la
        GROUP BY la.tier
        ORDER BY label
        """, nativeQuery = true)
    List<LvInt> sumPointsByTier();

    interface LvInt {
        String getLabel();
        Integer getValue();
    }
}
