package com.pablo.ecommerce.compra;

import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EmailEntregaServiceTests {
    @Test
    @SuppressWarnings("unchecked")
    void enviaStatusPeloSpringMailEMarcaFilaComoEntregue() {
        EmailNotificacaoRepository repositorio = mock(EmailNotificacaoRepository.class);
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        JavaMailSender sender = mock(JavaMailSender.class);
        EmailNotificacao email = new EmailNotificacao();
        email.setId(1L);
        email.setPedidoId(42L);
        email.setStatus(StatusPedido.PAGO);
        email.setDestinatario("cliente@example.com");
        email.setProximaTentativa(Instant.now().minusSeconds(1));
        when(repositorio.findByIdForUpdate(1L)).thenReturn(Optional.of(email));
        when(provider.getObject()).thenReturn(sender);
        new EmailEntregaService(repositorio, provider, "loja@example.com").enviar(1L);
        verify(sender).send(any(SimpleMailMessage.class));
        assertThat(email.getEnviadoEm()).isNotNull();
    }
}
