import { useState } from 'react'
import { money } from './api'

const labels = { AGUARDANDO_PAGAMENTO: 'Aguardando pagamento', PAGO: 'Pagamento confirmado', EM_PREPARACAO: 'Em preparação', ENVIADO: 'Enviado', ENTREGUE: 'Entregue', CANCELADO: 'Cancelado', EXPIRADO: 'Expirado' }
const active = ['AGUARDANDO_PAGAMENTO', 'PAGO', 'EM_PREPARACAO', 'ENVIADO']
const paid = ['PAGO', 'EM_PREPARACAO', 'ENVIADO', 'ENTREGUE']
const steps = ['AGUARDANDO_PAGAMENTO', 'PAGO', 'EM_PREPARACAO', 'ENVIADO', 'ENTREGUE']
const messages = { AGUARDANDO_PAGAMENTO: 'O pedido aguarda a confirmação do pagamento.', PAGO: 'Pagamento confirmado. A loja dará sequência ao seu pedido.', EM_PREPARACAO: 'Seu pedido está sendo preparado para envio.', ENVIADO: 'Seu pedido foi enviado. Aguarde a atualização de entrega.', ENTREGUE: 'A entrega foi concluída.', CANCELADO: 'Este pedido foi cancelado.', EXPIRADO: 'O prazo para pagamento deste pedido terminou.' }

export default function Orders({ orders, loading, error, onRefresh, onShop, onPay, onDownload, busy, isDemo, authenticated, onLogin }) {
  const [filter, setFilter] = useState('all')
  const [search, setSearch] = useState('')
  const [expanded, setExpanded] = useState(null)
  const sorted = [...orders].sort((a, b) => new Date(b.criadoEm) - new Date(a.criadoEm))
  const visible = sorted.filter(order => (filter === 'all' || (filter === 'active' ? active.includes(order.status) : !active.includes(order.status))) && `${order.id} ${order.itens.map(item => item.nome).join(' ')}`.toLocaleLowerCase('pt-BR').includes(search.trim().toLocaleLowerCase('pt-BR')))
  return <section className="inner-page container orders-page">
    <button className="text-back" onClick={onShop}>← Voltar à vitrine</button>
    <div className="orders-heading"><div className="page-heading"><span className="eyebrow">SUA HISTÓRIA NA CAZUMA</span><h1>Meus pedidos<span>.</span></h1><p>Acompanhe a entrega e consulte seu histórico de compras.</p></div>{authenticated && <button className="button button-outline" onClick={onRefresh} disabled={loading}> {loading ? 'Atualizando…' : 'Atualizar pedidos'}</button>}</div>
    {!authenticated ? <div className="empty-state"><h3>Entre para acompanhar seus pedidos.</h3><p>O histórico de compras está vinculado à sua conta.</p><button className="button button-dark" onClick={onLogin}>Entrar ou criar conta</button></div> : <>
      <div className="orders-stats"><div><strong>{orders.length}</strong><span>Pedidos registrados</span></div><div><strong>{orders.filter(order => active.includes(order.status)).length}</strong><span>Em andamento</span></div><div><strong>{orders.filter(order => order.status === 'ENTREGUE').length}</strong><span>Entregues</span></div></div>
      <div className="orders-toolbar"><div className="category-tabs" aria-label="Filtrar pedidos">{[['all', 'Todos os pedidos'], ['active', 'Em andamento'], ['history', 'Histórico de compras']].map(([key, label]) => <button key={key} className={filter === key ? 'active' : ''} aria-pressed={filter === key} onClick={() => setFilter(key)}>{label}</button>)}</div><input aria-label="Buscar por número do pedido ou produto" placeholder="Buscar pedido ou produto" value={search} onChange={event => setSearch(event.target.value)} /></div>
      {loading && <p role="status">Carregando pedidos…</p>}
      {error ? <div className="empty-state" role="alert"><h3>Não foi possível atualizar os pedidos.</h3><p>{error}</p><button className="button button-dark" onClick={onRefresh} disabled={loading}>Tentar novamente</button></div> : !loading && !visible.length ? <div className="empty-state"><h3>{orders.length ? 'Nenhum pedido encontrado.' : 'Sua história começa com a primeira compra.'}</h3><p>{orders.length ? 'Tente outro filtro ou busque outro produto.' : 'Seus pedidos e detalhes da entrega aparecerão aqui.'}</p><button className="button button-dark" onClick={onShop}>Explorar produtos</button></div> : <div className="orders-list">{visible.map(order => {
        const physical = order.itens.some(item => item.tipo === 'FISICO')
        const terminal = ['CANCELADO', 'EXPIRADO'].includes(order.status)
        const open = expanded === order.id
        return <article className="order-card" key={order.id}>
          <div className="order-card-head"><div><span className="eyebrow">PEDIDO #{order.id}</span><h3>{new Date(order.criadoEm).toLocaleDateString('pt-BR')}</h3></div><span className={`status-pill status-${order.status.toLowerCase()}`}>{labels[order.status] || order.status}</span></div>
          <div className="order-names">{order.itens.map(item => `${item.quantidade}× ${item.nome}`).join(' · ')}</div>
          <div className="delivery-progress"><strong>{physical ? 'Acompanhamento da entrega' : 'Entrega digital'}</strong><p>{physical || terminal || !paid.includes(order.status) ? messages[order.status] : 'Pagamento confirmado. Seus arquivos estão disponíveis para download.'}</p>
            {physical && !terminal && <ol className="order-steps" aria-label="Etapas do pedido">{steps.map((step, index) => <li key={step} className={index <= steps.indexOf(order.status) ? 'complete' : ''} aria-current={step === order.status ? 'step' : undefined}><span>{index < steps.indexOf(order.status) ? '✓' : index + 1}</span>{labels[step]}</li>)}</ol>}
          </div>
          <div className="order-card-foot"><span>{order.itens.reduce((sum, item) => sum + item.quantidade, 0)} item(ns) · Frete: {money(order.frete)}</span><strong>{money(order.total)}</strong></div>
          <div className="order-actions"><button className="button button-outline" aria-expanded={open} aria-controls={`order-details-${order.id}`} onClick={() => setExpanded(open ? null : order.id)}>{open ? 'Ocultar detalhes' : 'Ver detalhes da compra'}</button>{isDemo === false && order.status === 'AGUARDANDO_PAGAMENTO' && <button className="button button-dark" onClick={() => onPay(order)} disabled={busy}>Pagar pedido</button>}</div>
          {open && <div className="order-details" id={`order-details-${order.id}`}><h4>Itens da compra</h4>{order.itens.map(item => <div className="order-detail-item" key={item.id}><div><strong>{item.nome}</strong><small>{item.tipo === 'DIGITAL' ? 'Digital' : 'Físico'} · {item.quantidade} × {money(item.precoUnitario)}</small></div><strong>{money(item.subtotal)}</strong></div>)}<div className="order-totals"><span>Produtos: {money(order.subtotal)}</span><span>Frete: {money(order.frete)}</span><strong>Total: {money(order.total)}</strong></div>{physical && order.entrega && <div className="delivery-address"><h4>Endereço de entrega</h4><p>{order.entrega.logradouro}, {order.entrega.numero}{order.entrega.complemento && ` · ${order.entrega.complemento}`}<br />{order.entrega.bairro} · {order.entrega.cidade} / {order.entrega.uf}<br />CEP {order.entrega.cep}</p><small>O acompanhamento mostra o status registrado pela loja.</small></div>}</div>}
          {order.itens.some(item => item.tipo === 'DIGITAL') && <div className="order-actions">{order.itens.filter(item => item.tipo === 'DIGITAL').map(item => <button key={item.id} className="button button-outline" onClick={() => onDownload(order, item)} disabled={busy || !paid.includes(order.status)}>Baixar {item.nome}</button>)}</div>}
        </article>
      })}</div>}
    </>}
  </section>
}
