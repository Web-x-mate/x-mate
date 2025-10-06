package xmate.com.service.procurement;

import org.springframework.data.domain.*;
import xmate.com.domain.procurement.Batch;

import java.util.List;

public interface BatchService {
    Page<Batch> search(Long variantId, Long poItemId, Pageable pageable);
    List<Batch> byVariant(Long variantId);
    Batch get(Long id);
    Batch create(Batch b);
    Batch update(Long id, Batch b);
    void delete(Long id);
}
