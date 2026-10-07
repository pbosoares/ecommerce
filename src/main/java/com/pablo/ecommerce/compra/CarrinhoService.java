package com.pablo.ecommerce.compra;

import java.math.BigDecimal;
import java.util.List;
import com.pablo.ecommerce.produto.Produto;
import com.pablo.ecommerce.produto.ProdutoRepository;
import com.pablo.ecommerce.produto.TipoProduto;
import com.pablo.ecommerce.usuario.Usuario;
import com.pablo.ecommerce.usuario.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CarrinhoService {
    private final CarrinhoItemRepository itens;
    private final ProdutoRepository produtos;
    private final UsuarioRepository usuarios;

    public CarrinhoService(CarrinhoItemRepository itens, ProdutoRepository produtos, UsuarioRepository usuarios) {
        this.itens = itens;
        this.produtos = produtos;
        this.usuarios = usuarios;
    }

    public Long usuarioId(Jwt jwt) {
        try {
            return Long.valueOf(jwt.getSubject());
        } catch (NumberFormatException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario invalido");
        }
    }

    public Usuario bloquearUsuario(Long usuarioId) {
        return usuarios.findByIdForUpdate(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario nao encontrado"));
    }

    @Transactional(readOnly = true)
    public CarrinhoResponse buscar(Long usuarioId) {
        if (!usuarios.existsById(usuarioId)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario nao encontrado");
        }
        return resposta(itens.findByUsuarioIdOrderById(usuarioId));
    }

    @Transactional
    public CarrinhoResponse adicionar(Long usuarioId, ItemCarrinhoRequest request) {
        Usuario usuario = bloquearUsuario(usuarioId);
        Produto produto = produtos.findById(request.produtoId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Produto nao encontrado"));
        CarrinhoItem item = itens.findByUsuarioIdAndProdutoId(usuarioId, produto.getId())
                .orElseGet(() -> {
                    CarrinhoItem novo = new CarrinhoItem();
                    novo.setUsuario(usuario);
                    novo.setProduto(produto);
                    return novo;
                });
        int quantidade;
        try {
            quantidade = Math.addExact(item.getQuantidade(), request.quantidade());
        } catch (ArithmeticException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantidade invalida");
        }
        validarQuantidade(produto, quantidade);
        item.setQuantidade(quantidade);
        itens.save(item);
        return resposta(itens.findByUsuarioIdOrderById(usuarioId));
    }

    @Transactional
    public CarrinhoResponse alterar(Long usuarioId, Long produtoId, QuantidadeRequest request) {
        bloquearUsuario(usuarioId);
        CarrinhoItem item = itens.findByUsuarioIdAndProdutoId(usuarioId, produtoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item nao encontrado"));
        validarQuantidade(item.getProduto(), request.quantidade());
        item.setQuantidade(request.quantidade());
        return resposta(itens.findByUsuarioIdOrderById(usuarioId));
    }

    @Transactional
    public void remover(Long usuarioId, Long produtoId) {
        bloquearUsuario(usuarioId);
        CarrinhoItem item = itens.findByUsuarioIdAndProdutoId(usuarioId, produtoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item nao encontrado"));
        itens.delete(item);
    }

    public void validarQuantidade(Produto produto, int quantidade) {
        if (quantidade < 1 || quantidade > 999) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantidade deve estar entre 1 e 999");
        }
        if (produto.getTipo() == TipoProduto.FISICO && quantidade > produto.getEstoque()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Estoque insuficiente");
        }
    }

    public CarrinhoResponse resposta(List<CarrinhoItem> items) {
        var linhas = items.stream().map(item -> {
            Produto produto = item.getProduto();
            BigDecimal preco = produto.getPreco();
            return new CarrinhoResponse.Item(produto.getId(), produto.getNome(), produto.getSku(),
                    produto.getTipo(), item.getQuantidade(), preco,
                    preco.multiply(BigDecimal.valueOf(item.getQuantidade())));
        }).toList();
        BigDecimal subtotal = linhas.stream().map(CarrinhoResponse.Item::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CarrinhoResponse(linhas, subtotal);
    }
}
