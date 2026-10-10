package com.pablo.ecommerce.usuario;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnderecoSalvoRepository extends JpaRepository<EnderecoSalvo, Long> {
    List<EnderecoSalvo> findByUsuarioIdOrderById(Long usuarioId);
    Optional<EnderecoSalvo> findByIdAndUsuarioId(Long id, Long usuarioId);
}
