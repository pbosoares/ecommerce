package com.pablo.ecommerce.compra;

import java.time.Instant;
import org.springframework.stereotype.Service;

@Service
public class EmailNotificacaoService {
    private final EmailNotificacaoRepository emails;

    public EmailNotificacaoService(EmailNotificacaoRepository emails) {
        this.emails = emails;
    }

    public void agendar(Pedido pedido) {
        if (emails.existsByPedidoIdAndStatus(pedido.getId(), pedido.getStatus())) return;
        EmailNotificacao email = new EmailNotificacao();
        email.setPedidoId(pedido.getId());
        email.setStatus(pedido.getStatus());
        email.setDestinatario(pedido.getUsuario().getEmail());
        email.setProximaTentativa(Instant.now());
        emails.save(email);
    }
}
