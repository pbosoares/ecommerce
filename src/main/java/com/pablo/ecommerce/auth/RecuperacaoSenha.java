package com.pablo.ecommerce.auth;

import java.time.Instant;
import com.pablo.ecommerce.usuario.Usuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class RecuperacaoSenha {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(nullable = false)
    private Usuario usuario;
    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash;
    @Column(nullable = false)
    private Instant criadoEm;
    @Column(nullable = false)
    private Instant expiraEm;
    private Instant usadoEm;
}
