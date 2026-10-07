package com.pablo.ecommerce.compra;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import com.pablo.ecommerce.usuario.Usuario;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
public class Pedido {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Usuario usuario;

    @Setter
    private Instant criadoEm;

    @Setter
    @Enumerated(EnumType.STRING)
    private StatusPedido status;

    @Setter
    private BigDecimal subtotal;

    @Setter
    private BigDecimal frete;

    @Setter
    private BigDecimal total;

    @Setter
    @Embedded
    private EnderecoEntrega entrega;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PedidoItem> itens = new ArrayList<>();

    public void adicionar(PedidoItem item) {
        item.setPedido(this);
        itens.add(item);
    }
}
