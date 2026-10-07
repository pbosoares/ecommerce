package com.pablo.ecommerce.produto;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DigitalArquivoRepository extends JpaRepository<DigitalArquivo, Long> {
    Optional<DigitalArquivo> findByProdutoId(Long produtoId);
}
