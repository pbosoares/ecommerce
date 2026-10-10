package com.pablo.ecommerce.usuario;

import com.pablo.ecommerce.compra.EnderecoEntrega;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class EnderecoSalvo {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;
    @Column(nullable = false, length = 40)
    private String apelido;
    @Embedded
    private EnderecoEntrega endereco;
}
