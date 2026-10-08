package com.pablo.ecommerce.compra;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class ResendEmailClient {
    private static final URI ENDPOINT = URI.create("https://api.resend.com/emails");
    private final HttpClient http;
    private final URI endpoint;
    private final ObjectMapper json;
    private final String chave;
    private final String remetente;

    @Autowired
    public ResendEmailClient(ObjectMapper json, @Value("${app.resend.api-key:}") String chave,
            @Value("${app.email.from:}") String remetente) {
        this(json, chave, remetente, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build(), ENDPOINT);
    }

    ResendEmailClient(ObjectMapper json, String chave, String remetente, HttpClient http, URI endpoint) {
        this.json = json;
        this.chave = chave;
        this.remetente = remetente;
        this.http = http;
        this.endpoint = endpoint;
    }

    public boolean configurado() {
        return !chave.isBlank() && !remetente.isBlank();
    }

    public void enviar(EmailNotificacao email) throws IOException, InterruptedException {
        if (!configurado()) throw new IOException("Resend nao configurado");
        String assunto = "Cazuma - pedido #" + email.getPedidoId();
        String texto = "Seu pedido #" + email.getPedidoId() + " agora está: "
                + email.getStatus().name().replace('_', ' ') + ".";
        String payload;
        try {
            payload = json.writeValueAsString(new Mensagem(remetente, List.of(email.getDestinatario()), assunto, texto));
        } catch (Exception exception) {
            throw new IOException("Falha ao preparar e-mail", exception);
        }
        HttpRequest request = HttpRequest.newBuilder(endpoint).timeout(Duration.ofSeconds(12))
                .header("Authorization", "Bearer " + chave)
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", "cazuma-email-" + email.getId())
                .POST(HttpRequest.BodyPublishers.ofString(payload)).build();
        HttpResponse<String> resposta = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (resposta.statusCode() / 100 != 2) {
            throw new IOException("Resend respondeu HTTP " + resposta.statusCode());
        }
        try {
            if (json.readTree(resposta.body()).path("id").asText().isBlank()) {
                throw new IOException("Resend nao confirmou o envio");
            }
        } catch (RuntimeException exception) {
            throw new IOException("Resposta invalida do Resend", exception);
        }
    }

    private record Mensagem(String from, List<String> to, String subject, String text) {}
}
