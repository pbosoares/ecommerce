package com.pablo.ecommerce.compra;

import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EmailEntregaServiceTests {
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
