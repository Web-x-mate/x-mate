// xmate/com/security/RecaptchaService.java
package xmate.com.security;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
public class RecaptchaService {
    @Value("${app.recaptcha.secret}")
    private String secret;

    private final RestClient http = RestClient.create();

    @Data
    static class VerifyRes {
        private boolean success;
        @JsonAlias("error-codes") private List<String> errorCodes;
    }

    public boolean verify(String token, String remoteIp){
        var form = new LinkedMultiValueMap<String, String>();
        form.add("secret", secret);
        form.add("response", token);
        if (remoteIp != null) form.add("remoteip", remoteIp);

        var res = http.post()
                .uri("https://www.google.com/recaptcha/api/siteverify")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(VerifyRes.class);

        return res != null && res.isSuccess();
    }
}
