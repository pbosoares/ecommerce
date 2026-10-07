package com.pablo.ecommerce.compra;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StripeWebhookController {
    private final StripeCheckoutService stripe;
    private final PedidoService pedidos;

    public StripeWebhookController(StripeCheckoutService stripe, PedidoService pedidos) {
        this.stripe = stripe;
        this.pedidos = pedidos;
    }

    @PostMapping("/webhooks/stripe")
    public ResponseEntity<Void> receber(@RequestBody byte[] payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String signature) {
        var event = stripe.receberEvento(payload, signature);
        if (event != null) {
            if ("checkout.session.expired".equals(event.type())) {
                pedidos.expirarPagamento(event.pedidoId(), event.sessionId());
            } else if (event.paid()) {
                pedidos.confirmarPagamento(event.pedidoId(), event.sessionId(), event.centavos(), event.moeda());
            }
        }
        return ResponseEntity.ok().build();
    }
}
