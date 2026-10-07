package com.pablo.ecommerce.compra;

import java.math.BigDecimal;
import com.pablo.ecommerce.produto.TipoProduto;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
public class PedidoItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Pedido pedido;

    @Setter private Long produtoId;
    @Setter private String nome;
    @Setter private String sku;
    @Setter private int quantidade;
    @Setter @Enumerated(EnumType.STRING) private TipoProduto tipo;
    @Setter private BigDecimal precoUnitario;
    @Setter private BigDecimal subtotal;
    @Setter private String arquivoKey;
    @Setter private String arquivoNome;
}
