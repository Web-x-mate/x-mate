// src/main/java/xmate/com/service/procurement/SupplierService.java
package xmate.com.service.procurement;

import org.springframework.data.domain.*;
import xmate.com.domain.procurement.Supplier;

public interface SupplierService {
    Page<Supplier> search(String q, Pageable pageable);
    Supplier get(Long id);
    Supplier create(Supplier s);
    Supplier update(Long id, Supplier s);
    void delete(Long id);
}
