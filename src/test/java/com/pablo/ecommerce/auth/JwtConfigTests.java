package com.pablo.ecommerce.auth;

import com.pablo.ecommerce.config.JwtConfig;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtConfigTests {
    @Test
    void rejeitaChaveVaziaCurtaOuBase64Invalido() {
        for (String secret : new String[]{"", "YWJj", "invalida!"}) {
            assertThatThrownBy(() -> new JwtConfig().jwtKey(secret)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    void rejeitaValidadeNaoPositiva() {
        for (long ttl : new long[]{0, -1}) {
            assertThatThrownBy(() -> new TokenService(null, "ecommerce", ttl))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
