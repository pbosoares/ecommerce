package com.pablo.ecommerce.produto;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {
    List<Produto> findByCategoriaSlug(String slug);
    Optional<Produto> findBySku(String sku);

}
