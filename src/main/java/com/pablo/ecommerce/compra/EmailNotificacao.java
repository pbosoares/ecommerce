package com.pablo.ecommerce.compra;

import java.time.Instant;
import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"pedido_id", "status"}))
public class EmailNotificacao {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "pedido_id")
    private Long pedidoId;
    @Column(name = "usuario_id", unique = true)
    private Long usuarioId;
    private String nomeDestinatario;
    @Enumerated(EnumType.STRING)
    private StatusPedido status;
    private String destinatario;
    private int tentativas;
    private Instant proximaTentativa;
    private Instant enviadoEm;
}
