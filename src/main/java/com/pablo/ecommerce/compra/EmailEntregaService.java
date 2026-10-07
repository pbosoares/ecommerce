package com.pablo.ecommerce.compra;

import java.time.Instant;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmailEntregaService {
    private final EmailNotificacaoRepository emails;
    private final ObjectProvider<JavaMailSender> mailSender;
    private final String remetente;

    public EmailEntregaService(EmailNotificacaoRepository emails, ObjectProvider<JavaMailSender> mailSender,
            @Value("${app.email.from:}") String remetente) {
        this.emails = emails;
        this.mailSender = mailSender;
        this.remetente = remetente;
    }

    public boolean configurado() { return !remetente.isBlank() && mailSender.getIfAvailable() != null; }

    @Transactional
    public void enviar(Long id) {
        EmailNotificacao email = emails.findByIdForUpdate(id).orElse(null);
        if (email == null || email.getEnviadoEm() != null || email.getProximaTentativa().isAfter(Instant.now())) return;
        try {
            SimpleMailMessage mensagem = new SimpleMailMessage();
            mensagem.setFrom(remetente);
            mensagem.setTo(email.getDestinatario());
            mensagem.setSubject("Cazuma - pedido #" + email.getPedidoId());
            mensagem.setText("Seu pedido #" + email.getPedidoId() + " agora está: "
                    + email.getStatus().name().replace('_', ' ') + ".");
            mailSender.getObject().send(mensagem);
            email.setEnviadoEm(Instant.now());
        } catch (org.springframework.mail.MailException exception) {
            email.setTentativas(email.getTentativas() + 1);
            email.setProximaTentativa(Instant.now().plusSeconds(
                    Math.min(3600, 60L << Math.min(email.getTentativas(), 5))));
        }
    }
}
