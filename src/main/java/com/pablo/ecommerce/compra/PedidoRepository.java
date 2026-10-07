package com.pablo.ecommerce.compra;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    List<Pedido> findByUsuarioIdOrderByCriadoEmDesc(Long usuarioId);
    Optional<Pedido> findByIdAndUsuarioId(Long id, Long usuarioId);
}
