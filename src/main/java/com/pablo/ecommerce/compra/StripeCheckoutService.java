package com.pablo.ecommerce.compra;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.core.JacksonException;
import java.io.IOException;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class StripeCheckoutService {
    private static final URI SESSIONS = URI.create("https://api.stripe.com/v1/checkout/sessions");
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final ObjectMapper json;
    private final String apiKey;
    private final String webhookSecret;
    private final String successUrl;
    private final String cancelUrl;

    public StripeCheckoutService(ObjectMapper json, @Value("${app.stripe.secret-key:}") String apiKey,
            @Value("${app.stripe.webhook-secret:}") String webhookSecret,
            @Value("${app.stripe.success-url:}") String successUrl,
            @Value("${app.stripe.cancel-url:}") String cancelUrl) {
        this.json = json;
        this.apiKey = apiKey;
        this.webhookSecret = webhookSecret;
        this.successUrl = successUrl;
        this.cancelUrl = cancelUrl;
    }

    public CheckoutResponse criarSessao(Pedido pedido) {
        if (apiKey.isBlank()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Stripe nao configurado");
        long centavos = pedido.getTotal().setScale(2, RoundingMode.UNNECESSARY).movePointRight(2).longValueExact();
        List<String> form = new ArrayList<>();
        campo(form, "mode", "payment");
        campo(form, "payment_method_types[0]", "card");
        campo(form, "client_reference_id", pedido.getId().toString());
        campo(form, "customer_email", pedido.getUsuario().getEmail());
        campo(form, "success_url", successUrl);
        campo(form, "cancel_url", cancelUrl);
        campo(form, "line_items[0][price_data][currency]", "brl");
        campo(form, "line_items[0][price_data][product_data][name]", "Pedido Cazuma #" + pedido.getId());
        campo(form, "line_items[0][price_data][unit_amount]", Long.toString(centavos));
        campo(form, "line_items[0][quantity]", "1");
        // Stable across retries using the same Stripe idempotency key.
        campo(form, "expires_at", Long.toString(pedido.getCriadoEm().plus(Duration.ofMinutes(65)).getEpochSecond()));
        HttpRequest request = HttpRequest.newBuilder(SESSIONS).timeout(Duration.ofSeconds(12))
                .header("Authorization", "Bearer " + apiKey)
                .header("Idempotency-Key", pedido.getCheckoutRequestKey())
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(String.join("&", form))).build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Nao foi possivel iniciar o pagamento");
            }
            JsonNode body = json.readTree(response.body());
            String id = body.path("id").asText();
            String url = body.path("url").asText();
            if (!id.startsWith("cs_") || !url.startsWith("https://checkout.stripe.com/")) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Resposta de pagamento invalida");
            }
            return new CheckoutResponse(id, url);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Servico de pagamento indisponivel", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Servico de pagamento interrompido", exception);
        }
    }

    public StripeEvent receberEvento(byte[] payload, String signature) {
        if (webhookSecret.isBlank()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Webhook nao configurado");
        if (payload.length > 65536 || !assinaturaValida(payload, signature)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Assinatura de webhook invalida");
        }
        try {
            JsonNode event = json.readTree(payload);
            JsonNode session = event.path("data").path("object");
            String type = event.path("type").asText();
            if (!"checkout.session.completed".equals(type)
                    && !"checkout.session.async_payment_succeeded".equals(type)
                    && !"checkout.session.expired".equals(type)) return null;
            if (!"checkout.session".equals(session.path("object").asText())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Objeto de pagamento invalido");
            }
            Long pedidoId = Long.valueOf(session.path("client_reference_id").asText());
            String sessionId = session.path("id").asText();
            return new StripeEvent(type, pedidoId, sessionId,
                    "paid".equals(session.path("payment_status").asText()),
                    session.path("amount_total").longValue(), session.path("currency").asText());
        } catch (JacksonException | NumberFormatException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Evento de pagamento invalido", exception);
        }
    }

    public record StripeEvent(String type, Long pedidoId, String sessionId, boolean paid,
            long centavos, String moeda) {}

    private boolean assinaturaValida(byte[] payload, String header) {
        if (header == null) return false;
        String timestamp = null;
        List<String> signatures = new ArrayList<>();
        for (String part : header.split(",")) {
            String[] pair = part.trim().split("=", 2);
            if (pair.length == 2 && "t".equals(pair[0])) timestamp = pair[1];
            if (pair.length == 2 && "v1".equals(pair[0])) signatures.add(pair[1]);
        }
        if (timestamp == null || signatures.isEmpty()) return false;
        try {
            long seconds = Long.parseLong(timestamp);
            if (Math.abs(Instant.now().getEpochSecond() - seconds) > 300) return false;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            mac.update((timestamp + ".").getBytes(StandardCharsets.UTF_8));
            byte[] expected = mac.doFinal(payload);
            for (String candidate : signatures) {
                if (candidate.length() != 64) continue;
                if (MessageDigest.isEqual(expected, HexFormat.of().parseHex(candidate))) return true;
            }
            return false;
        } catch (Exception exception) {
            return false;
        }
    }

    private void campo(List<String> form, String key, String value) {
        form.add(URLEncoder.encode(key, StandardCharsets.UTF_8) + "=" + URLEncoder.encode(value, StandardCharsets.UTF_8));
    }
}
