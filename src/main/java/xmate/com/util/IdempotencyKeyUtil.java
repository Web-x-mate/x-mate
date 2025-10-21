// src/main/java/util/IdempotencyKeyUtil.java
package xmate.com.util;

import org.springframework.http.HttpHeaders;

public class IdempotencyKeyUtil {
    public static String resolve(HttpHeaders headers){
        String k = headers.getFirst("Idempotency-Key");
        return (k==null || k.isBlank()) ? java.util.UUID.randomUUID().toString() : k;
    }
}
