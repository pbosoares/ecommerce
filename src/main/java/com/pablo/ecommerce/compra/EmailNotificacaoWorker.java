package com.pablo.ecommerce.compra;

import java.time.Instant;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class EmailNotificacaoWorker {
    private final EmailNotificacaoRepository emails;
    private final EmailEntregaService entrega;

    public EmailNotificacaoWorker(EmailNotificacaoRepository emails, EmailEntregaService entrega) {
        this.emails = emails;
        this.entrega = entrega;
    }

    @Scheduled(fixedDelay = 60000)
    public void executar() {
        if (!entrega.configurado()) return;
        for (EmailNotificacao email : emails.findTop20ByEnviadoEmIsNullAndProximaTentativaBeforeOrderById(Instant.now())) {
            entrega.enviar(email.getId());
        }
    }
}
