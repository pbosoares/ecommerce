package com.pablo.ecommerce.compra;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmailNotificacaoRepository extends JpaRepository<EmailNotificacao, Long> {
    boolean existsByPedidoIdAndStatus(Long pedidoId, StatusPedido status);
    List<EmailNotificacao> findByDestinatarioAndRecuperacaoUrlIsNotNull(String destinatario);
    List<EmailNotificacao> findTop20ByEnviadoEmIsNullAndProximaTentativaBeforeOrderById(Instant agora);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from EmailNotificacao e where e.id = :id")
    Optional<EmailNotificacao> findByIdForUpdate(@Param("id") Long id);
}
