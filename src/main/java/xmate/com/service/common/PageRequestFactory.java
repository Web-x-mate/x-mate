package xmate.com.service.common;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
/** Helper tạo Pageable có sort mặc định */
public class PageRequestFactory {
    public static PageRequest of(int page, int size){ 
        return PageRequest.of(Math.max(0,page), Math.min(Math.max(1,size), 200)); 
    }
    public static PageRequest ofSorted(int page, int size, String sortBy, boolean desc){
        return PageRequest.of(Math.max(0,page), Math.min(Math.max(1,size), 200),
                desc? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending());
    }
}
