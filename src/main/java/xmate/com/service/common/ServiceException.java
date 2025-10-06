package xmate.com.service.common;
/** RuntimeException cơ bản cho tầng service */
public class ServiceException extends RuntimeException { 
    public ServiceException(String m) { super(m); }
}
