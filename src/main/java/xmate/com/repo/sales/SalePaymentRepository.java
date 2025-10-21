package xmate.com.repo.sales;

import org.springframework.data.jpa.repository.JpaRepository;
import xmate.com.entity.sales.Order;
import xmate.com.entity.sales.Payment;
import java.util.List;
import java.util.Optional;

public interface SalePaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByOrder(Order order);
    Optional<Payment> findByOrder_Code(String code);   // OK: nested property
    List<Payment> findByProofStatus(Payment.ProofStatus status);
}
