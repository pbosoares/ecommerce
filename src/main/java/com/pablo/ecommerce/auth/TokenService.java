package com.pablo.ecommerce.auth;

import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import com.pablo.ecommerce.usuario.Usuario;

@Service
public class TokenService {
    private final JwtEncoder encoder;
    private final String issuer;
    private final long ttlSeconds;

    public TokenService(JwtEncoder encoder, @Value("${app.jwt.issuer}") String issuer,
                        @Value("${app.jwt.ttl-seconds}") long ttlSeconds) {
        if (ttlSeconds <= 0) {
            throw new IllegalArgumentException("A validade do JWT deve ser positiva");
        }
        this.encoder = encoder;
        this.issuer = issuer;
        this.ttlSeconds = ttlSeconds;
    }

    public TokenResponse emitir(Usuario usuario) {
        Instant now = Instant.now();
        var claims = JwtClaimsSet.builder().issuer(issuer).subject(usuario.getId().toString())
                .claim("papel", usuario.getPapel().name())
                .issuedAt(now).expiresAt(now.plusSeconds(ttlSeconds)).build();
        var header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new TokenResponse(token, "Bearer", ttlSeconds);
    }
}
