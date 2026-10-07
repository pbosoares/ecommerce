package com.pablo.ecommerce.produto;

import java.util.List;
import java.util.Optional;
import com.pablo.ecommerce.categoria.Categoria;
import com.pablo.ecommerce.categoria.CategoriaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.stereotype.Service;

@Service
public class ProdutoService {

    private final ProdutoRepository produtoRepository;
    private final CategoriaRepository categoriaRepository;

    public ProdutoService(ProdutoRepository produtoRepository, CategoriaRepository categoriaRepository) {
        this.produtoRepository = produtoRepository;
        this.categoriaRepository = categoriaRepository;
    }

    public Produto salvar(ProdutoRequest request) {
        Produto produto = new Produto();
        preencher(produto, request);
        return produtoRepository.save(produto);

    }

    public List<Produto> listarTodos() {
        return produtoRepository.findAll();
    }

    public List<Produto> listarPorCategoria(String slug) {
        return produtoRepository.findByCategoriaSlug(slug);
    }

    public Optional<Produto> buscarPorId(Long id) {
        return produtoRepository.findById(id);

    }

    public void excluir(Long id) {
        produtoRepository.deleteById(id);
    }

    public Produto atualizar(Long id, ProdutoRequest request) {
        Produto produto = produtoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Produto nao encontrado"));
        preencher(produto, request);
        return produtoRepository.save(produto);
    }

    private void preencher(Produto produto, ProdutoRequest request) {
        Categoria categoria = categoriaRepository.findById(request.categoriaId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Categoria nao encontrada"));
        produto.setNome(request.nome());
        produto.setDescricao(request.descricao());
        produto.setPreco(request.preco());
        produto.setEstoque(request.estoque());
        produto.setCategoria(categoria);
    }
}
