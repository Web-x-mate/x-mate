package xmate.com.service.common;
/** Ném khi không tìm thấy entity theo tiêu chí */
public class NotFoundException extends ServiceException {
    public NotFoundException(String m) { super(m); }
    public static NotFoundException ofEntity(String entity, Object id){
        return new NotFoundException(entity + " not found: " + id);
    }
}
