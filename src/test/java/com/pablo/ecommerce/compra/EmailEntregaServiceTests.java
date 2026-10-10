package com.pablo.ecommerce.compra;

import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EmailEntregaServiceTests {
    @Test
    void falhaNoEnvioDeBoasVindasPermaneceNaFila() throws Exception {
        var repositorio = mock(EmailNotificacaoRepository.class);
        var resend = mock(ResendEmailClient.class);
        var email = new EmailNotificacao();
        email.setId(2L);
        email.setUsuarioId(3L);
        email.setProximaTentativa(Instant.now().minusSeconds(1));
        when(repositorio.findByIdForUpdate(2L)).thenReturn(Optional.of(email));
        doThrow(new java.io.IOException("indisponível")).when(resend).enviar(email);
        new EmailEntregaService(repositorio, resend).enviar(2L);
        assertThat(email.getEnviadoEm()).isNull();
        assertThat(email.getTentativas()).isEqualTo(1);
        assertThat(email.getProximaTentativa()).isAfter(Instant.now());
    }

    @Test
    void enviaStatusPeloResendEMarcaFilaComoEntregue() throws Exception {
        EmailNotificacaoRepository repositorio = mock(EmailNotificacaoRepository.class);
        ResendEmailClient resend = mock(ResendEmailClient.class);
        EmailNotificacao email = new EmailNotificacao();
        email.setId(1L);
        email.setPedidoId(42L);
        email.setStatus(StatusPedido.PAGO);
        email.setDestinatario("cliente@example.com");
        email.setProximaTentativa(Instant.now().minusSeconds(1));
        when(repositorio.findByIdForUpdate(1L)).thenReturn(Optional.of(email));
        new EmailEntregaService(repositorio, resend).enviar(1L);
        verify(resend).enviar(email);
        assertThat(email.getEnviadoEm()).isNotNull();
    }
}
