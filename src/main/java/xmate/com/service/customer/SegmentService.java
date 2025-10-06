// src/main/java/xmate/com/service/customer/SegmentService.java
package xmate.com.service.customer;

import org.springframework.data.domain.*;
import xmate.com.domain.customer.Segment;

import java.util.List;

public interface SegmentService {
    Page<Segment> search(String q, Pageable pageable);
    List<Segment> all();
    Segment get(Long id);
    Segment create(Segment s);          // validate unique name
    Segment update(Long id, Segment s); // validate unique name
    void delete(Long id);
}

