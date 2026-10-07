package com.pablo.ecommerce.compra;

import java.util.List;
import java.util.Optional;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    List<Pedido> findByUsuarioIdOrderByCriadoEmDesc(Long usuarioId);
    Optional<Pedido> findByIdAndUsuarioId(Long id, Long usuarioId);
    Optional<Pedido> findByUsuarioIdAndIdempotencyKey(Long usuarioId, String idempotencyKey);
    List<Pedido> findTop100ByStatusAndCheckoutSessionIdIsNullAndCriadoEmBefore(
            StatusPedido status, Instant limite);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Pedido p where p.id = :id")
    Optional<Pedido> findByIdForUpdate(@Param("id") Long id);
}
