package com.pablo.ecommerce.compra;

import java.time.Instant;
import java.io.IOException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmailEntregaService {
    private final EmailNotificacaoRepository emails;
    private final ResendEmailClient resend;

    public EmailEntregaService(EmailNotificacaoRepository emails, ResendEmailClient resend) {
        this.emails = emails;
        this.resend = resend;
    }

    public boolean configurado() { return resend.configurado(); }

    @Transactional
    public void enviar(Long id) {
        EmailNotificacao email = emails.findByIdForUpdate(id).orElse(null);
        if (email == null || email.getEnviadoEm() != null || email.getProximaTentativa().isAfter(Instant.now())) return;
        if (email.getRecuperacaoExpiraEm() != null && !email.getRecuperacaoExpiraEm().isAfter(Instant.now())) {
            // Discard expired links and erase the queued credential without sending an unusable e-mail.
            email.setRecuperacaoUrl(null);
            email.setEnviadoEm(Instant.now());
            return;
        }
        try {
            resend.enviar(email);
            email.setEnviadoEm(Instant.now());
            email.setRecuperacaoUrl(null);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            reagendar(email);
        } catch (IOException exception) {
            reagendar(email);
        }
    }

    private void reagendar(EmailNotificacao email) {
            email.setTentativas(email.getTentativas() + 1);
            email.setProximaTentativa(Instant.now().plusSeconds(
                    Math.min(3600, 60L << Math.min(email.getTentativas(), 5))));
    }
}
