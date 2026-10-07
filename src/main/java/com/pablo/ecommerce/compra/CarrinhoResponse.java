package com.pablo.ecommerce.compra;

import java.math.BigDecimal;
import java.util.List;
import com.pablo.ecommerce.produto.TipoProduto;

public record CarrinhoResponse(List<Item> itens, BigDecimal subtotal) {
    public record Item(Long produtoId, String nome, String sku, TipoProduto tipo,
            int quantidade, BigDecimal precoUnitario, BigDecimal subtotal) {
    }
}
