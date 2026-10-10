package com.pablo.ecommerce.auth;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecuperacaoSenhaRepository extends JpaRepository<RecuperacaoSenha, Long> {
    Optional<RecuperacaoSenha> findByTokenHash(String tokenHash);
    List<RecuperacaoSenha> findByUsuarioIdAndUsadoEmIsNull(Long usuarioId);
}
