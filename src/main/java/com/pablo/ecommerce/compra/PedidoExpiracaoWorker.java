package com.pablo.ecommerce.compra;

import java.time.Instant;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PedidoExpiracaoWorker {
    private final PedidoRepository pedidos;
    private final PedidoService servico;

    public PedidoExpiracaoWorker(PedidoRepository pedidos, PedidoService servico) {
        this.pedidos = pedidos;
        this.servico = servico;
    }

    @Scheduled(fixedDelay = 60000)
    public void executar() {
        for (Pedido pedido : pedidos.findTop100ByStatusAndCheckoutSessionIdIsNullAndCriadoEmBefore(
                StatusPedido.AGUARDANDO_PAGAMENTO, Instant.now().minusSeconds(1800))) {
            servico.expirarSemCheckout(pedido.getId());
        }
    }
}
