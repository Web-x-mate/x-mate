package xmate.com.app;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Loggable {
    String action() default "";     // CREATE, UPDATE, DELETE...
    String entityType() default ""; // PRODUCT, ORDER...
}
