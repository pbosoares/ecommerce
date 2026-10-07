package com.pablo.ecommerce.compra;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.UUID;
import com.pablo.ecommerce.produto.Produto;
import com.pablo.ecommerce.produto.ProdutoRepository;
import com.pablo.ecommerce.produto.DigitalArquivoRepository;
import com.pablo.ecommerce.produto.TipoProduto;
import com.pablo.ecommerce.usuario.Usuario;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PedidoService {
    private final PedidoRepository pedidos;
    private final CarrinhoItemRepository itens;
    private final CarrinhoService carrinho;
    private final FreteService frete;
    private final ProdutoRepository produtos;
    private final StripeCheckoutService stripe;
    private final EmailNotificacaoService emails;
    private final DigitalArquivoRepository arquivos;

    public PedidoService(PedidoRepository pedidos, CarrinhoItemRepository itens, CarrinhoService carrinho,
            FreteService frete, ProdutoRepository produtos, StripeCheckoutService stripe,
            EmailNotificacaoService emails, DigitalArquivoRepository arquivos) {
        this.pedidos = pedidos;
        this.itens = itens;
        this.carrinho = carrinho;
        this.frete = frete;
        this.produtos = produtos;
        this.stripe = stripe;
        this.emails = emails;
        this.arquivos = arquivos;
    }

    @Transactional
    public PedidoResponse criar(Long usuarioId, CriarPedidoRequest request, String idempotencyKey) {
        Usuario usuario = carrinho.bloquearUsuario(usuarioId);
        if (idempotencyKey != null) {
            if (!idempotencyKey.matches("[A-Za-z0-9_-]{8,80}")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chave de idempotencia invalida");
            }
            var anterior = pedidos.findByUsuarioIdAndIdempotencyKey(usuarioId, idempotencyKey);
            if (anterior.isPresent()) return PedidoResponse.from(anterior.get());
        }
        List<CarrinhoItem> linhas = itens.findByUsuarioIdOrderById(usuarioId).stream()
                .sorted(Comparator.comparing(item -> item.getProduto().getId())).toList();
        if (linhas.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Carrinho vazio");
        }
        boolean temFisico = linhas.stream().anyMatch(item -> item.getProduto().getTipo() == TipoProduto.FISICO);
        if (temFisico && (request == null || request.entrega() == null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Endereco de entrega obrigatorio");
        }
        Pedido pedido = new Pedido();
        pedido.setUsuario(usuario);
        pedido.setIdempotencyKey(idempotencyKey);
        pedido.setCheckoutRequestKey(UUID.randomUUID().toString());
        pedido.setCriadoEm(Instant.now());
        pedido.setStatus(StatusPedido.AGUARDANDO_PAGAMENTO);
        if (temFisico) pedido.setEntrega(request.entrega().toEntity());
        BigDecimal subtotal = BigDecimal.ZERO;
        List<Produto> reservados = new ArrayList<>();
        for (CarrinhoItem linha : linhas) {
            Produto produto = produtos.findByIdForUpdate(linha.getProduto().getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Produto indisponivel"));
            carrinho.validarQuantidade(produto, linha.getQuantidade());
            reservados.add(produto);
            PedidoItem item = new PedidoItem();
            item.setProdutoId(produto.getId());
            item.setNome(produto.getNome());
            item.setSku(produto.getSku());
            item.setTipo(produto.getTipo());
            if (produto.getTipo() == TipoProduto.DIGITAL) {
                arquivos.findByProdutoId(produto.getId()).ifPresent(arquivo -> {
                    item.setArquivoKey(arquivo.getChave());
                    item.setArquivoNome(arquivo.getNome());
                });
            }
            item.setQuantidade(linha.getQuantidade());
            item.setPrecoUnitario(produto.getPreco());
            item.setSubtotal(produto.getPreco().multiply(BigDecimal.valueOf(linha.getQuantidade())));
            subtotal = subtotal.add(item.getSubtotal());
            pedido.adicionar(item);
        }
        pedido.setSubtotal(subtotal);
        if (temFisico) {
            CotacaoFreteResponse cotacao = frete.cotar(linhas, pedido.getEntrega().getCep());
            pedido.setFrete(cotacao.frete());
            pedido.setTotal(cotacao.total());
        } else {
            pedido.setFrete(BigDecimal.ZERO);
            pedido.setTotal(subtotal);
        }
        for (int indice = 0; indice < linhas.size(); indice++) {
            Produto produto = reservados.get(indice);
            if (produto.getTipo() == TipoProduto.FISICO) {
                produto.setEstoque(produto.getEstoque() - linhas.get(indice).getQuantidade());
            }
        }
        pedido.setEstoqueReservado(true);
        Pedido salvo = pedidos.saveAndFlush(pedido);
        emails.agendar(salvo);
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

    @Transactional(readOnly = true)
    public Page<PedidoResponse> listarAdmin(int pagina) {
        return pedidos.findAll(PageRequest.of(pagina, 30, Sort.by(Sort.Direction.DESC, "criadoEm")))
                .map(PedidoResponse::from);
    }

    @Transactional
    public PedidoResponse cancelar(Long usuarioId, Long id) {
        Pedido pedido = bloquear(id);
        if (!pedido.getUsuario().getId().equals(usuarioId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido nao encontrado");
        }
        if (pedido.getStatus() != StatusPedido.AGUARDANDO_PAGAMENTO || pedido.getCheckoutSessionId() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Pedido nao pode ser cancelado nesta etapa");
        }
        liberarEstoque(pedido);
        pedido.setStatus(StatusPedido.CANCELADO);
        emails.agendar(pedido);
        return PedidoResponse.from(pedido);
    }

    @Transactional
    public CheckoutResponse checkout(Long usuarioId, Long id) {
        Pedido pedido = bloquear(id);
        if (!pedido.getUsuario().getId().equals(usuarioId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido nao encontrado");
        }
        if (pedido.getStatus() != StatusPedido.AGUARDANDO_PAGAMENTO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Pedido nao aguarda pagamento");
        }
        if (pedido.getCheckoutSessionId() == null
                && pedido.getCriadoEm().isBefore(Instant.now().minusSeconds(1800))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Prazo para iniciar o pagamento expirou");
        }
        if (!pedido.isEstoqueReservado()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Pedido antigo sem reserva; refaca a compra");
        }
        if (pedido.getItens().stream().anyMatch(item -> item.getTipo() == TipoProduto.DIGITAL
                && item.getArquivoKey() == null)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Produto digital sem arquivo para entrega");
        }
        if (pedido.getCheckoutSessionId() == null) {
            if (pedido.getCheckoutRequestKey() == null) pedido.setCheckoutRequestKey(UUID.randomUUID().toString());
            CheckoutResponse sessao = stripe.criarSessao(pedido);
            pedido.setCheckoutSessionId(sessao.sessionId());
            pedido.setCheckoutUrl(sessao.url());
        }
        return new CheckoutResponse(pedido.getCheckoutSessionId(), pedido.getCheckoutUrl());
    }

    @Transactional
    public void confirmarPagamento(Long id, String sessionId, long centavos, String moeda) {
        Pedido pedido = bloquear(id);
        if (!sessionId.equals(pedido.getCheckoutSessionId()) || !"brl".equalsIgnoreCase(moeda)
                || pedido.getTotal().movePointRight(2).longValueExact() != centavos) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Pagamento nao corresponde ao pedido");
        }
        if (pedido.getStatus() == StatusPedido.PAGO || pedido.getStatus() == StatusPedido.EM_PREPARACAO
                || pedido.getStatus() == StatusPedido.ENVIADO || pedido.getStatus() == StatusPedido.ENTREGUE) return;
        if (pedido.getStatus() != StatusPedido.AGUARDANDO_PAGAMENTO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Pagamento de pedido encerrado requer conciliacao");
        }
        pedido.setStatus(StatusPedido.PAGO);
        pedido.setPagoEm(Instant.now());
        emails.agendar(pedido);
    }

    @Transactional
    public void expirarPagamento(Long id, String sessionId) {
        Pedido pedido = bloquear(id);
        if (!sessionId.equals(pedido.getCheckoutSessionId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Sessao nao corresponde ao pedido");
        }
        if (pedido.getStatus() != StatusPedido.AGUARDANDO_PAGAMENTO) return;
        liberarEstoque(pedido);
        pedido.setStatus(StatusPedido.EXPIRADO);
        emails.agendar(pedido);
    }

    @Transactional
    public void expirarSemCheckout(Long id) {
        Pedido pedido = bloquear(id);
        if (pedido.getStatus() != StatusPedido.AGUARDANDO_PAGAMENTO
                || pedido.getCheckoutSessionId() != null
                || pedido.getCriadoEm().isAfter(Instant.now().minusSeconds(1800))) return;
        liberarEstoque(pedido);
        pedido.setStatus(StatusPedido.EXPIRADO);
        emails.agendar(pedido);
    }

    @Transactional
    public PedidoResponse avancar(Long id, StatusPedido destino) {
        Pedido pedido = bloquear(id);
        StatusPedido atual = pedido.getStatus();
        boolean permitido = (atual == StatusPedido.PAGO && destino == StatusPedido.EM_PREPARACAO)
                || (atual == StatusPedido.EM_PREPARACAO && destino == StatusPedido.ENVIADO)
                || (atual == StatusPedido.ENVIADO && destino == StatusPedido.ENTREGUE);
        if (!permitido) throw new ResponseStatusException(HttpStatus.CONFLICT, "Transicao de status invalida");
        pedido.setStatus(destino);
        emails.agendar(pedido);
        return PedidoResponse.from(pedido);
    }

    private Pedido bloquear(Long id) {
        return pedidos.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido nao encontrado"));
    }

    private void liberarEstoque(Pedido pedido) {
        if (!pedido.isEstoqueReservado()) return;
        for (PedidoItem item : pedido.getItens()) {
            if (item.getTipo() != TipoProduto.FISICO) continue;
            produtos.findByIdForUpdate(item.getProdutoId())
                    .ifPresent(produto -> produto.setEstoque(Math.addExact(produto.getEstoque(), item.getQuantidade())));
        }
        pedido.setEstoqueReservado(false);
    }
}
