package com.pablo.ecommerce.produto;

import java.util.List;
import java.util.Optional;
import java.util.ArrayList;
import java.util.HashMap;
import java.net.URI;
import org.springframework.dao.DataIntegrityViolationException;
import com.pablo.ecommerce.categoria.Categoria;
import com.pablo.ecommerce.categoria.CategoriaRepository;
import com.pablo.ecommerce.compra.CarrinhoItemRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProdutoService {

    private final ProdutoRepository produtoRepository;
    private final CategoriaRepository categoriaRepository;
    private final CarrinhoItemRepository carrinhoItens;

    public ProdutoService(ProdutoRepository produtoRepository, CategoriaRepository categoriaRepository,
            CarrinhoItemRepository carrinhoItens) {
        this.produtoRepository = produtoRepository;
        this.categoriaRepository = categoriaRepository;
        this.carrinhoItens = carrinhoItens;
    }

    public Produto salvar(ProdutoRequest request) {
        Produto produto = new Produto();
        preencher(produto, request);
        return persistir(produto);

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

    @Transactional
    public void excluir(Long id) {
        carrinhoItens.deleteByProdutoId(id);
        produtoRepository.deleteById(id);
    }

    public Produto atualizar(Long id, ProdutoRequest request) {
        Produto produto = produtoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Produto nao encontrado"));
        preencher(produto, request);
        return persistir(produto);
    }

    private Produto persistir(Produto produto) {
        try {
            return produtoRepository.saveAndFlush(produto);
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Produto com SKU duplicado", exception);
        }
    }

    private void preencher(Produto produto, ProdutoRequest request) {
        Categoria categoria = categoriaRepository.findById(request.categoriaId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Categoria nao encontrada"));
        TipoProduto tipo = request.tipo() == null ? TipoProduto.FISICO : request.tipo();
        if (tipo == TipoProduto.FISICO && request.estoque() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Estoque e obrigatorio para produto fisico");
        }
        if (tipo == TipoProduto.DIGITAL && ((request.estoque() != null && request.estoque() != 0)
                || request.pesoGramas() != null || request.alturaCm() != null
                || request.larguraCm() != null || request.comprimentoCm() != null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Produto digital nao possui estoque ou dimensoes de envio");
        }
        if (request.imagens() != null) {
            for (String image : request.imagens()) {
                try {
                    URI uri = URI.create(image);
                    if (!("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                            || uri.getHost() == null || image.length() > 2048) {
                        throw new IllegalArgumentException();
                    }
                } catch (IllegalArgumentException exception) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Imagem deve ter URL HTTP ou HTTPS valida");
                }
            }
        }
        if (request.atributos() != null && request.atributos().entrySet().stream()
                .anyMatch(entry -> entry.getKey().length() > 60 || entry.getValue().length() > 255)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Atributo muito longo");
        }
        String sku = request.sku() == null ? null : request.sku().trim();
        if (sku != null && produtoRepository.findBySku(sku)
                .filter(existing -> !existing.getId().equals(produto.getId())).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Produto com SKU duplicado");
        }
        produto.setNome(request.nome());
        produto.setDescricao(request.descricao());
        produto.setPreco(request.preco());
        produto.setEstoque(tipo == TipoProduto.DIGITAL ? 0 : request.estoque());
        produto.setCategoria(categoria);
        produto.setTipo(tipo);
        produto.setSku(sku);
        produto.setImagens(request.imagens() == null ? new ArrayList<>() : new ArrayList<>(request.imagens()));
        produto.setAtributos(request.atributos() == null ? new HashMap<>() : new HashMap<>(request.atributos()));
        produto.setPesoGramas(request.pesoGramas());
        produto.setAlturaCm(request.alturaCm());
        produto.setLarguraCm(request.larguraCm());
        produto.setComprimentoCm(request.comprimentoCm());
    }
}
