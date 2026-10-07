package com.pablo.ecommerce.compra;

import com.jayway.jsonpath.JsonPath;
import com.pablo.ecommerce.produto.ProdutoRepository;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
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

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:ecommerce-webhook;DB_CLOSE_DELAY=-1",
        "app.stripe.webhook-secret=whsec_test"
})
@ActiveProfiles("demo")
@AutoConfigureMockMvc
class StripeWebhookIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired PedidoRepository pedidos;
    @Autowired ProdutoRepository produtos;

    @Test
    void aceitaApenasEventoAssinadoDoPedidoECancelaReservaAoExpirar() throws Exception {
        var cadastro = mvc.perform(post("/usuarios").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Teste\",\"email\":\"webhook@demo.invalid\",\"senha\":\"senha-segura\"}"))
                .andExpect(status().isOk()).andReturn();
        var login = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"webhook@demo.invalid\",\"senha\":\"senha-segura\"}"))
                .andExpect(status().isOk()).andReturn();
        String token = "Bearer " + JsonPath.read(login.getResponse().getContentAsString(), "$.accessToken");
        var produto = produtos.findAll().stream().filter(p -> p.getTipo().name().equals("FISICO")).findFirst().orElseThrow();
        int inicial = produto.getEstoque();
        mvc.perform(post("/carrinho/itens").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"produtoId\":" + produto.getId() + ",\"quantidade\":1}"))
                .andExpect(status().isOk());
        var result = mvc.perform(post("/pedidos").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"entrega\":{\"cep\":\"01001000\",\"logradouro\":\"Rua A\",\"numero\":\"1\",\"bairro\":\"Centro\",\"cidade\":\"Sao Paulo\",\"uf\":\"SP\"}}"))
                .andExpect(status().isCreated()).andReturn();
        Long id = Long.valueOf((Integer) JsonPath.read(result.getResponse().getContentAsString(), "$.id"));
        var pedido = pedidos.findById(id).orElseThrow();
        pedido.setCheckoutSessionId("cs_test_integration");
        pedidos.saveAndFlush(pedido);
        long centavos = pedido.getTotal().movePointRight(2).longValueExact();
        String evento = evento("checkout.session.completed", id, centavos, "paid");

        mvc.perform(post("/webhooks/stripe").header("Stripe-Signature", "t=1,v1=abc")
                .contentType(MediaType.APPLICATION_JSON).content(evento))
                .andExpect(status().isBadRequest());
        assertThat(pedidos.findById(id).orElseThrow().getStatus()).isEqualTo(StatusPedido.AGUARDANDO_PAGAMENTO);
        mvc.perform(post("/webhooks/stripe").header("Stripe-Signature", assinar(evento))
                .contentType(MediaType.APPLICATION_JSON).content(evento))
                .andExpect(status().isOk());
        mvc.perform(post("/webhooks/stripe").header("Stripe-Signature", assinar(evento))
                .contentType(MediaType.APPLICATION_JSON).content(evento))
                .andExpect(status().isOk());
        assertThat(pedidos.findById(id).orElseThrow().getStatus()).isEqualTo(StatusPedido.PAGO);
        assertThat(produtos.findById(produto.getId()).orElseThrow().getEstoque()).isEqualTo(inicial - 1);

        String expirado = evento("checkout.session.expired", id, centavos, "unpaid");
        mvc.perform(post("/webhooks/stripe").header("Stripe-Signature", assinar(expirado))
                .contentType(MediaType.APPLICATION_JSON).content(expirado))
                .andExpect(status().isOk());
        assertThat(pedidos.findById(id).orElseThrow().getStatus()).isEqualTo(StatusPedido.PAGO);
        assertThat(produtos.findById(produto.getId()).orElseThrow().getEstoque()).isEqualTo(inicial - 1);
    }

    private String evento(String tipo, long id, long centavos, String pagamento) {
        return "{\"type\":\"" + tipo + "\",\"data\":{\"object\":{\"object\":\"checkout.session\","
                + "\"client_reference_id\":\"" + id + "\",\"id\":\"cs_test_integration\","
                + "\"payment_status\":\"" + pagamento + "\",\"amount_total\":" + centavos
                + ",\"currency\":\"brl\"}}}";
    }

    private String assinar(String payload) throws Exception {
        String timestamp = Long.toString(Instant.now().getEpochSecond());
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec("whsec_test".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return "t=" + timestamp + ",v1=" + HexFormat.of().formatHex(
                mac.doFinal((timestamp + "." + payload).getBytes(StandardCharsets.UTF_8)));
    }
}
