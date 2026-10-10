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
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import tools.jackson.databind.ObjectMapper;

@Component
public class ResendEmailClient {
    private static final URI ENDPOINT = URI.create("https://api.resend.com/emails");
    private final HttpClient http;
    private final URI endpoint;
    private final ObjectMapper json;
    private final String chave;
    private final String remetente;
    private String lojaUrl = "";
    private boolean pagamentoTeste;

    @Autowired
    public ResendEmailClient(ObjectMapper json, @Value("${app.resend.api-key:}") String chave,
            @Value("${app.email.from:}") String remetente,
            @Value("${app.stripe.success-url:}") String retornoUrl, Environment environment) {
        this(json, chave, remetente, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build(), ENDPOINT);
        if (!retornoUrl.isBlank()) {
            URI retorno = URI.create(retornoUrl);
            if ("https".equals(retorno.getScheme()) && retorno.getHost() != null) {
                lojaUrl = "https://" + retorno.getAuthority() + "/";
            }
        }
        pagamentoTeste = environment.acceptsProfiles(Profiles.of("sandbox", "demo"));
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
                + (email.getStatus() == null ? "" : email.getStatus().name().replace('_', ' ')) + ".";
        if (email.getUsuarioId() != null) {
            assunto = "Boas-vindas à Cazuma!";
            texto = "Olá, " + email.getNomeDestinatario() + "!\n\n"
                    + "Sua conta na Cazuma foi criada. Explore a vitrine, escolha seus favoritos e acompanhe os pedidos pela sua conta.\n\n"
                    + (lojaUrl.isBlank() ? "" : "Visite a loja: " + lojaUrl + "\n\n")
                    + (pagamentoTeste ? "Estamos em modo de teste: os produtos são fictícios, sem cobrança ou entrega real.\n\n" : "")
                    + "Bom ter você por aqui!\nEquipe Cazuma";
        }
        if (email.getRecuperacaoUrl() != null) {
            assunto = "Redefina sua senha na Cazuma";
            texto = "Olá, " + email.getNomeDestinatario() + "!\n\n"
                    + "Recebemos uma solicitação para redefinir sua senha. Acesse o link abaixo:\n\n"
                    + email.getRecuperacaoUrl() + "\n\n"
                    + "O link vale por 30 minutos a partir da solicitação e pode ser usado uma única vez. "
                    + "Se você pediu outro link, use o mais recente.\n\n"
                    + "Se não foi você, ignore este e-mail. Sua senha permanece a mesma.\nEquipe Cazuma";
        }
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
