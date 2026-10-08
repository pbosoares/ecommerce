package com.pablo.ecommerce.compra;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

class ResendEmailClientTests {
    @Test
    void enviaJsonComChaveDeIdempotencia() throws Exception {
        HttpServer servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicReference<String> corpo = new AtomicReference<>();
        AtomicReference<String> autorizacao = new AtomicReference<>();
        AtomicReference<String> idempotencia = new AtomicReference<>();
        servidor.createContext("/emails", troca -> {
            corpo.set(new String(troca.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            autorizacao.set(troca.getRequestHeaders().getFirst("Authorization"));
            idempotencia.set(troca.getRequestHeaders().getFirst("Idempotency-Key"));
            byte[] resposta = "{\"id\":\"email-123\"}".getBytes(StandardCharsets.UTF_8);
            troca.sendResponseHeaders(200, resposta.length);
            try (var saida = troca.getResponseBody()) { saida.write(resposta); }
        });
        servidor.start();
        try {
            EmailNotificacao email = new EmailNotificacao();
            email.setId(7L);
            email.setPedidoId(42L);
            email.setStatus(StatusPedido.PAGO);
            email.setDestinatario("cliente@example.com");
            var client = new ResendEmailClient(new ObjectMapper(), "re_teste", "loja@example.com",
                    HttpClient.newHttpClient(), URI.create("http://127.0.0.1:" + servidor.getAddress().getPort() + "/emails"));
            client.enviar(email);
            assertThat(autorizacao.get()).isEqualTo("Bearer re_teste");
            assertThat(idempotencia.get()).isEqualTo("cazuma-email-7");
            assertThat(corpo.get()).contains("\"from\":\"loja@example.com\"")
                    .contains("\"cliente@example.com\"")
                    .contains("\"text\":\"Seu pedido #42 agora está: PAGO.\"");
        } finally {
            servidor.stop(0);
        }
    }
}
