package com.pablo.ecommerce.produto;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/produtos")
public class ProdutoController {
    private final ProdutoService produtoService;

    public ProdutoController(ProdutoService produtoService) {
        this.produtoService = produtoService;
    }

    @GetMapping
    public List<Produto> listarTodos(@RequestParam(required = false) String categoria) {
        return categoria == null ? produtoService.listarTodos() : produtoService.listarPorCategoria(categoria);

    }

    @DeleteMapping("/{id}")
    public void excluir(@PathVariable Long id) {
        produtoService.excluir(id);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Produto> buscarPorId(@PathVariable Long id) {
        return produtoService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Produto salvar(@Valid @RequestBody ProdutoRequest produto) {
        return produtoService.salvar(produto);
    }

    @PutMapping("/{id}")
    public Produto atualizar(
            @PathVariable Long id,
            @Valid @RequestBody ProdutoRequest produtoAtualizado) {
        return produtoService.atualizar(id, produtoAtualizado);

    }

}
