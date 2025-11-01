package com.example.springboot4demo;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/token")
public class TokenController {

    private final JwtEncoder jwtEncoder;

    public TokenController(JwtEncoder jwtEncoder) {
        this.jwtEncoder = jwtEncoder;
    }

    @PostMapping("/admin")
    public TokenResponse issueAdminToken(Authentication authentication) {
        return issueToken(authentication, "admin", "ROLE_ADMIN");
    }

    @PostMapping("/user")
    public TokenResponse issueUserToken(Authentication authentication) {
        return issueToken(authentication, "user", "ROLE_USER");
    }

    private TokenResponse issueToken(Authentication authentication, String scope, String authority) {
        Instant now = Instant.now();
        long expiry = 3600L;

        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer("self")
            .issuedAt(now)
            .expiresAt(now.plusSeconds(expiry))
            .subject(authentication.getName())
            .claim("scope", scope)
            .claim("authorities", authority)
            .build();

        String token = this.jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();

        return new TokenResponse(token, expiry, authority);
    }

    public record TokenResponse(String token, long expiresIn, String authority) {}
}
