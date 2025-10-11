package xmate.com.security.oauth;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    @Override
    public OAuth2User loadUser(OAuth2UserRequest req) {
        OAuth2User delegate = super.loadUser(req);
        Map<String,Object> attrs = delegate.getAttributes();
        String email = (String) attrs.get("email");
        if (email == null || email.isBlank())
            email = String.valueOf(attrs.getOrDefault("preferred_username", attrs.get("sub")));
        String name = (String) attrs.getOrDefault("name", email);
        attrs.put("email", email);
        attrs.putIfAbsent("name", name);

        return new DefaultOAuth2User(
                Set.of(new SimpleGrantedAuthority("USER")),
                attrs,
                "email"
        );
    }
}
