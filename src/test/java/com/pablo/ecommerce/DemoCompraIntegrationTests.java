package com.pablo.ecommerce;

import com.jayway.jsonpath.JsonPath;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("demo")
class DemoCompraIntegrationTests {
    @Autowired MockMvc mvc;

    @Test
    void catalogoFicticioPermiteSimularCompraSemPagamento() throws Exception {
        mvc.perform(get("/loja/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.demo").value(true));
        mvc.perform(get("/categorias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4));
        var catalogo = mvc.perform(get("/produtos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6))
                .andReturn();
        List<?> digitais = JsonPath.read(catalogo.getResponse().getContentAsString(), "$[?(@.tipo == 'DIGITAL')]");
        assertThat(digitais).hasSize(1);
        Number produtoId = JsonPath.read(catalogo.getResponse().getContentAsString(), "$[0].id");

        mvc.perform(post("/usuarios").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Cliente Demo\",\"email\":\"cliente@demo.invalid\",\"senha\":\"senha-segura\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.papel").value("CLIENTE"));
        var login = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"cliente@demo.invalid\",\"senha\":\"senha-segura\"}"))
                .andExpect(status().isOk()).andReturn();
        String token = "Bearer " + JsonPath.read(login.getResponse().getContentAsString(), "$.accessToken");

        mvc.perform(post("/carrinho/itens").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"produtoId\":" + produtoId + ",\"quantidade\":1}"))
                .andExpect(status().isOk());
        mvc.perform(post("/frete/cotacoes").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"cep\":\"01001000\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.frete").value(15));
        mvc.perform(post("/pedidos").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"entrega\":{\"cep\":\"01001000\",\"logradouro\":\"Praca da Se\",\"numero\":\"1\",\"bairro\":\"Se\",\"cidade\":\"Sao Paulo\",\"uf\":\"SP\"}}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("AGUARDANDO_PAGAMENTO"))
                .andExpect(jsonPath("$.frete").value(15));
    }
}
