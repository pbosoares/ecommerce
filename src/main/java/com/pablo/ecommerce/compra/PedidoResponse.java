package com.pablo.ecommerce.compra;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import com.pablo.ecommerce.produto.TipoProduto;

public record PedidoResponse(Long id, Instant criadoEm, StatusPedido status,
        BigDecimal subtotal, BigDecimal frete, BigDecimal total,
        EnderecoEntrega entrega, List<Item> itens) {
    public record Item(Long produtoId, String nome, String sku, TipoProduto tipo,
            int quantidade, BigDecimal precoUnitario, BigDecimal subtotal) {
    }

    public static PedidoResponse from(Pedido pedido) {
        return new PedidoResponse(pedido.getId(), pedido.getCriadoEm(), pedido.getStatus(),
                pedido.getSubtotal(), pedido.getFrete(), pedido.getTotal(), pedido.getEntrega(),
                pedido.getItens().stream().map(item -> new Item(item.getProdutoId(), item.getNome(),
                        item.getSku(), item.getTipo(), item.getQuantidade(), item.getPrecoUnitario(),
                        item.getSubtotal())).toList());
    }
}
