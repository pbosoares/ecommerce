package com.pablo.ecommerce.compra;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CarrinhoItemRepository extends JpaRepository<CarrinhoItem, Long> {
    List<CarrinhoItem> findByUsuarioIdOrderById(Long usuarioId);
    Optional<CarrinhoItem> findByUsuarioIdAndProdutoId(Long usuarioId, Long produtoId);
    void deleteByUsuarioId(Long usuarioId);
    void deleteByProdutoId(Long produtoId);
}
