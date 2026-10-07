package com.pablo.ecommerce.categoria;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
    boolean existsBySlug(String slug);
}
