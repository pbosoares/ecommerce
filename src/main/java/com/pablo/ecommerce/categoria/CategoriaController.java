package com.pablo.ecommerce.categoria;

import java.util.List;

import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/categorias")
public class CategoriaController {
    private final CategoriaRepository categorias;

    public CategoriaController(CategoriaRepository categorias) {
        this.categorias = categorias;
    }

    @GetMapping
    public List<Categoria> listar() {
        return categorias.findAll(Sort.by("nome"));
    }

    @PostMapping
    public Categoria criar(@Valid @RequestBody CategoriaRequest request) {
        if (categorias.existsBySlug(request.slug())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Categoria ja existe");
        }
        Categoria categoria = new Categoria();
        categoria.setNome(request.nome().trim());
        categoria.setSlug(request.slug());
        try {
            return categorias.saveAndFlush(categoria);
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Categoria ja existe", exception);
        }
    }
}
