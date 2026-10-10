package com.pablo.ecommerce.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitFilterTests {
    @Test
    void limitaSolicitacaoERedefinicaoDeSenhaComoLogin() throws Exception {
        for (String path : new String[]{"/auth/recuperar-senha", "/auth/redefinir-senha"}) {
            RateLimitFilter filtro = new RateLimitFilter();
            for (int i = 0; i < 11; i++) {
                var request = new MockHttpServletRequest("POST", path);
                request.setRemoteAddr("192.0.2.2");
                var response = new MockHttpServletResponse();
                filtro.doFilter(request, response, new MockFilterChain());
                assertThat(response.getStatus()).isEqualTo(i == 10 ? 429 : 200);
            }
        }
    }
    @Test
    void bloqueiaOnzeLoginsNoMinutoSemBloquearLeitura() throws Exception {
        RateLimitFilter filtro = new RateLimitFilter();
        for (int i = 0; i < 11; i++) {
            var request = new MockHttpServletRequest("POST", "/auth/login");
            request.setRemoteAddr("192.0.2.1");
            var response = new MockHttpServletResponse();
            filtro.doFilter(request, response, new MockFilterChain());
            assertThat(response.getStatus()).isEqualTo(i == 10 ? 429 : 200);
            if (i == 10) assertThat(response.getHeader("Retry-After")).isNotBlank();
        }
        var leitura = new MockHttpServletRequest("GET", "/produtos");
        leitura.setRemoteAddr("192.0.2.1");
        var response = new MockHttpServletResponse();
        filtro.doFilter(leitura, response, new MockFilterChain());
        assertThat(response.getStatus()).isEqualTo(200);
    }
}
