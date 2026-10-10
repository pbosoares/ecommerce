package com.pablo.ecommerce.compra;

import java.time.Instant;
import com.pablo.ecommerce.usuario.Usuario;
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

    public void agendarBoasVindas(Usuario usuario) {
        EmailNotificacao email = new EmailNotificacao();
        email.setUsuarioId(usuario.getId());
        email.setNomeDestinatario(usuario.getNome());
        email.setDestinatario(usuario.getEmail());
        email.setProximaTentativa(Instant.now());
        emails.save(email);
    }

    public void agendarRecuperacao(Usuario usuario, String url, Instant expiraEm) {
        EmailNotificacao email = new EmailNotificacao();
        email.setDestinatario(usuario.getEmail());
        email.setNomeDestinatario(usuario.getNome());
        email.setRecuperacaoUrl(url);
        email.setRecuperacaoExpiraEm(expiraEm);
        email.setProximaTentativa(Instant.now());
        emails.save(email);
    }

    public void descartarRecuperacoes(Usuario usuario) {
        emails.findByDestinatarioAndRecuperacaoUrlIsNotNull(usuario.getEmail()).forEach(email -> {
            email.setRecuperacaoUrl(null);
            if (email.getEnviadoEm() == null) email.setEnviadoEm(Instant.now());
        });
    }
}
