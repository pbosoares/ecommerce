package com.pablo.ecommerce.compra;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import com.pablo.ecommerce.produto.Produto;
import com.pablo.ecommerce.produto.TipoProduto;
import com.pablo.ecommerce.usuario.Usuario;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PedidoService {
    private final PedidoRepository pedidos;
    private final CarrinhoItemRepository itens;
    private final CarrinhoService carrinho;

    public PedidoService(PedidoRepository pedidos, CarrinhoItemRepository itens, CarrinhoService carrinho) {
        this.pedidos = pedidos;
        this.itens = itens;
        this.carrinho = carrinho;
    }

    @Transactional
    public PedidoResponse criar(Long usuarioId, CriarPedidoRequest request) {
        Usuario usuario = carrinho.bloquearUsuario(usuarioId);
        List<CarrinhoItem> linhas = itens.findByUsuarioIdOrderById(usuarioId);
        if (linhas.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Carrinho vazio");
        }
        boolean temFisico = linhas.stream().anyMatch(item -> item.getProduto().getTipo() == TipoProduto.FISICO);
        if (temFisico && (request == null || request.entrega() == null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Endereco de entrega obrigatorio");
        }
        Pedido pedido = new Pedido();
        pedido.setUsuario(usuario);
        pedido.setCriadoEm(Instant.now());
        pedido.setStatus(temFisico ? StatusPedido.AGUARDANDO_FRETE : StatusPedido.AGUARDANDO_PAGAMENTO);
        if (temFisico) pedido.setEntrega(request.entrega().toEntity());
        BigDecimal subtotal = BigDecimal.ZERO;
        for (CarrinhoItem linha : linhas) {
            Produto produto = linha.getProduto();
            carrinho.validarQuantidade(produto, linha.getQuantidade());
            PedidoItem item = new PedidoItem();
            item.setProdutoId(produto.getId());
            item.setNome(produto.getNome());
            item.setSku(produto.getSku());
            item.setTipo(produto.getTipo());
            item.setQuantidade(linha.getQuantidade());
            item.setPrecoUnitario(produto.getPreco());
            item.setSubtotal(produto.getPreco().multiply(BigDecimal.valueOf(linha.getQuantidade())));
            subtotal = subtotal.add(item.getSubtotal());
            pedido.adicionar(item);
        }
        pedido.setSubtotal(subtotal);
        if (!temFisico) pedido.setTotal(subtotal);
        Pedido salvo = pedidos.saveAndFlush(pedido);
        itens.deleteAll(linhas);
        itens.flush();
        return PedidoResponse.from(salvo);
    }

    @Transactional(readOnly = true)
    public List<PedidoResponse> listar(Long usuarioId) {
        return pedidos.findByUsuarioIdOrderByCriadoEmDesc(usuarioId).stream()
                .map(PedidoResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public PedidoResponse buscar(Long usuarioId, Long id) {
        return pedidos.findByIdAndUsuarioId(id, usuarioId).map(PedidoResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido nao encontrado"));
    }
}
