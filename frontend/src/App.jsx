import { useEffect, useMemo, useState } from 'react'
import { money, request } from './api'

const emptyAddress = { cep: '', logradouro: '', numero: '', complemento: '', bairro: '', cidade: '', uf: '' }

function Icon({ name, size = 20, stroke = 1.8 }) {
  const paths = {
    search: <><circle cx="11" cy="11" r="7" /><path d="m16.5 16.5 4 4" /></>,
    bag: <><path d="M4 8h16l-1 12H5L4 8Z" /><path d="M9 9V6a3 3 0 0 1 6 0v3" /></>,
    user: <><circle cx="12" cy="8" r="3.5" /><path d="M5 20a7 7 0 0 1 14 0" /></>,
    arrow: <><path d="M4 12h16" /><path d="m14 6 6 6-6 6" /></>,
    back: <><path d="M20 12H4" /><path d="m10 6-6 6 6 6" /></>,
    close: <><path d="M5 5 19 19M19 5 5 19" /></>,
    plus: <><path d="M12 5v14M5 12h14" /></>,
    minus: <><path d="M5 12h14" /></>,
    pin: <><path d="M19 10c0 5-7 11-7 11S5 15 5 10a7 7 0 1 1 14 0Z" /><circle cx="12" cy="10" r="2" /></>,
    package: <><path d="m3 7 9-4 9 4v10l-9 4-9-4V7Z" /><path d="m3 7 9 4 9-4M12 11v10" /></>,
    spark: <><path d="m12 2 2.3 7.7L22 12l-7.7 2.3L12 22l-2.3-7.7L2 12l7.7-2.3L12 2Z" /></>,
    check: <><path d="m4 12 5 5L20 6" /></>,
    filter: <><path d="M4 7h16M7 12h10M10 17h4" /></>,
    trash: <><path d="M4 7h16M9 7V4h6v3M7 7l1 13h8l1-13M10 11v5M14 11v5" /></>,
    menu: <><path d="M4 7h16M4 12h16M4 17h16" /></>,
  }
  return <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={stroke} strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">{paths[name]}</svg>
}

function ProductVisual({ product, className = '' }) {
  const [broken, setBroken] = useState(false)
  const image = product?.imagens?.[0]
  useEffect(() => setBroken(false), [image])
  return <div className={`product-visual ${className}`}>
    {image && !broken
      ? <img src={image} alt={product.nome} onError={() => setBroken(true)} loading="lazy" />
      : <div className="visual-fallback"><span className="fallback-orb" /><Icon name={product?.tipo === 'DIGITAL' ? 'spark' : 'package'} size={42} stroke={1.35} /></div>}
  </div>
}

function Modal({ children, onClose, className = '' }) {
  useEffect(() => {
    const onKey = (event) => { if (event.key === 'Escape') onClose() }
    document.addEventListener('keydown', onKey)
    document.body.style.overflow = 'hidden'
    return () => { document.removeEventListener('keydown', onKey); document.body.style.overflow = '' }
  }, [onClose])
  return <div className="modal-backdrop" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose() }}>
    <div className={`modal ${className}`} role="dialog" aria-modal="true">
      {children}
    </div>
  </div>
}

function ProductCard({ product, onOpen, onAdd }) {
  return <article className="product-card">
    <button className="card-image-button" onClick={() => onOpen(product)} aria-label={`Ver ${product.nome}`}>
      <ProductVisual product={product} />
      <span className="product-type">{product.tipo === 'DIGITAL' ? 'Digital' : 'Produto físico'}</span>
    </button>
    <div className="card-info">
      <span className="card-category">{product.categoria?.nome || 'Descobertas'}</span>
      <button className="card-title" onClick={() => onOpen(product)}>{product.nome}</button>
      <strong className="card-price">{money(product.preco)}</strong>
      <span className="card-caption">{product.tipo === 'DIGITAL' ? 'Acesso após liberação' : 'Frete calculado no carrinho'}</span>
      <button className="card-add" onClick={() => onAdd(product)} disabled={product.tipo !== 'DIGITAL' && product.estoque < 1}>
        <Icon name="plus" size={18} /> {product.tipo !== 'DIGITAL' && product.estoque < 1 ? 'Indisponível' : 'Adicionar'}
      </button>
    </div>
  </article>
}

function AuthModal({ mode, setMode, onClose, onSubmit, busy }) {
  const [form, setForm] = useState({ nome: '', email: '', senha: '' })
  const signup = mode === 'register'
  return <Modal onClose={onClose} className="auth-modal">
    <button className="icon-button modal-close" onClick={onClose} aria-label="Fechar"><Icon name="close" /></button>
    <div className="auth-mark">c<span>.</span></div>
    <p className="eyebrow">Sua conta Cazuma</p>
    <h2>{signup ? 'Bom ter você por aqui.' : 'Que bom te ver de novo.'}</h2>
    <p className="muted">{signup ? 'Crie sua conta para guardar suas escolhas.' : 'Entre para continuar de onde parou.'}</p>
    <form onSubmit={(event) => { event.preventDefault(); onSubmit(form, signup) }} className="auth-form">
      {signup && <label>Seu nome<input required autoComplete="name" value={form.nome} onChange={(e) => setForm({ ...form, nome: e.target.value })} placeholder="Como podemos te chamar?" /></label>}
      <label>E-mail<input required type="email" autoComplete="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} placeholder="voce@exemplo.com" /></label>
      <label>Senha<input required type="password" minLength={8} autoComplete={signup ? 'new-password' : 'current-password'} value={form.senha} onChange={(e) => setForm({ ...form, senha: e.target.value })} placeholder="Mínimo de 8 caracteres" /></label>
      <button className="button button-dark full" disabled={busy}>{busy ? 'Aguarde…' : signup ? 'Criar minha conta' : 'Entrar na minha conta'} <Icon name="arrow" size={18} /></button>
    </form>
    <p className="auth-switch">{signup ? 'Já tem uma conta?' : 'Ainda não tem uma conta?'} <button onClick={() => setMode(signup ? 'login' : 'register')}>{signup ? 'Entrar' : 'Criar conta'}</button></p>
  </Modal>
}

export default function App() {
  const [products, setProducts] = useState([])
  const [categories, setCategories] = useState([])
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState('')
  const [search, setSearch] = useState('')
  const [activeCategory, setActiveCategory] = useState('todos')
  const [view, setView] = useState('home')
  const [selected, setSelected] = useState(null)
  const [token, setToken] = useState(() => sessionStorage.getItem('cazuma_token') || '')
  const [cart, setCart] = useState(null)
  const [orders, setOrders] = useState([])
  const [authOpen, setAuthOpen] = useState(false)
  const [authMode, setAuthMode] = useState('login')
  const [pendingProduct, setPendingProduct] = useState(null)
  const [busy, setBusy] = useState(false)
  const [toast, setToast] = useState('')
  const [cep, setCep] = useState('')
  const [quote, setQuote] = useState(null)
  const [address, setAddress] = useState(emptyAddress)

  const notify = (message) => setToast(message)
  useEffect(() => { if (!toast) return; const id = setTimeout(() => setToast(''), 4500); return () => clearTimeout(id) }, [toast])

  const loadCatalog = async () => {
    setLoading(true); setLoadError('')
    try {
      const [items, groups] = await Promise.all([request('/produtos'), request('/categorias')])
      setProducts(items); setCategories(groups)
    } catch (error) { setLoadError(error.message) }
    finally { setLoading(false) }
  }
  useEffect(() => { loadCatalog() }, [])
  useEffect(() => {
    if (token) request('/carrinho', { token }).then(setCart).catch(() => { sessionStorage.removeItem('cazuma_token'); setToken(''); setCart(null) })
  }, [token])

  const visibleProducts = useMemo(() => products.filter((product) => {
    const inCategory = activeCategory === 'todos' || product.categoria?.slug === activeCategory
    const inSearch = `${product.nome} ${product.descricao || ''}`.toLocaleLowerCase('pt-BR').includes(search.trim().toLocaleLowerCase('pt-BR'))
    return inCategory && inSearch
  }), [products, activeCategory, search])
  const count = cart?.itens?.reduce((sum, item) => sum + item.quantidade, 0) || 0
  const hasPhysical = cart?.itens?.some((item) => item.tipo === 'FISICO')

  const go = (next) => { setView(next); setSelected(null); window.scrollTo({ top: 0, behavior: 'smooth' }) }
  const requireAccount = (product = null) => { setPendingProduct(product); setAuthMode('login'); setAuthOpen(true) }
  const perform = async (fn) => {
    setBusy(true)
    try { await fn() }
    catch (error) { notify(error.message); if (error.message.includes('sessão terminou')) { sessionStorage.removeItem('cazuma_token'); setToken(''); setCart(null); setAuthOpen(true) } }
    finally { setBusy(false) }
  }
  const addProduct = async (product, authToken = token) => {
    if (!authToken) return requireAccount(product)
    await perform(async () => {
      const nextCart = await request('/carrinho/itens', { token: authToken, method: 'POST', body: JSON.stringify({ produtoId: product.id, quantidade: 1 }) })
      setCart(nextCart); setQuote(null); setSelected(null); notify(`${product.nome} foi para o carrinho.`)
    })
  }
  const submitAuth = (form, signup) => perform(async () => {
    if (signup) await request('/usuarios', { method: 'POST', body: JSON.stringify(form) })
    const response = await request('/auth/login', { method: 'POST', body: JSON.stringify({ email: form.email, senha: form.senha }) })
    sessionStorage.setItem('cazuma_token', response.accessToken)
    setToken(response.accessToken); setAuthOpen(false); notify('Você entrou na Cazuma.')
    if (pendingProduct) { const product = pendingProduct; setPendingProduct(null); await addProduct(product, response.accessToken) }
  })
  const changeQuantity = (item, quantity) => perform(async () => {
    if (quantity < 1) return removeItem(item)
    const nextCart = await request(`/carrinho/itens/${item.produtoId}`, { token, method: 'PUT', body: JSON.stringify({ quantidade: quantity }) })
    setCart(nextCart); setQuote(null)
  })
  const removeItem = (item) => perform(async () => {
    await request(`/carrinho/itens/${item.produtoId}`, { token, method: 'DELETE' })
    setCart(await request('/carrinho', { token })); setQuote(null)
  })
  const calculateShipping = () => perform(async () => {
    const clean = cep.replace(/\D/g, '')
    if (clean.length !== 8) throw new Error('Digite um CEP com 8 números.')
    const result = await request('/frete/cotacoes', { token, method: 'POST', body: JSON.stringify({ cep: clean }) })
    setQuote(result); setAddress((current) => ({ ...current, cep: clean }))
  })
  const placeOrder = () => perform(async () => {
    if (hasPhysical && !quote) throw new Error('Calcule o frete antes de registrar o pedido.')
    const body = hasPhysical ? { entrega: { ...address, cep: cep.replace(/\D/g, ''), uf: address.uf.toUpperCase() } } : {}
    const order = await request('/pedidos', { token, method: 'POST', body: JSON.stringify(body) })
    setCart(await request('/carrinho', { token })); setQuote(null); setCep(''); setAddress(emptyAddress)
    notify(`Pedido #${order.id} registrado. O pagamento ainda não está disponível.`)
    await loadOrders(); go('orders')
  })
  const loadOrders = async () => {
    if (!token) return requireAccount()
    try { setOrders(await request('/pedidos', { token })); go('orders') }
    catch (error) { notify(error.message) }
  }

  return <div className="site-shell">
    <div className="announcement"><span>Boas escolhas começam por aqui</span><span className="announcement-dot">✦</span><span>Produtos físicos e digitais em um só lugar</span></div>
    <header className="site-header">
      <div className="header-main container">
        <button className="brand" onClick={() => go('home')} aria-label="Cazuma, página inicial"><span className="brand-symbol">c<span className="brand-sun">●</span></span><span>cazuma<span className="brand-period">.</span></span></button>
        <label className="search-box"><Icon name="search" size={21} /><input value={search} onChange={(event) => { setSearch(event.target.value); if (view !== 'home') go('home') }} placeholder="O que você está procurando?" aria-label="Buscar produtos" /><span className="search-shortcut">Buscar</span></label>
        <div className="header-actions">
          <button className="header-action account-action" aria-label={token ? 'Meus pedidos' : 'Entrar ou cadastrar'} onClick={() => token ? loadOrders() : requireAccount()}><Icon name="user" /><span>{token ? 'Meus pedidos' : 'Entrar / Cadastrar'}</span></button>
          <button className="header-action cart-action" aria-label={`Carrinho com ${count} itens`} onClick={() => go('cart')}><Icon name="bag" /><span>Carrinho</span>{count > 0 && <b className="cart-count">{count}</b>}</button>
        </div>
      </div>
      <nav className="header-nav container" aria-label="Navegação principal">
        <button onClick={() => go('home')}><Icon name="menu" size={18} /> Todas as categorias</button>
        <span className="nav-divider" />
        <button onClick={() => { setActiveCategory('todos'); go('home') }}>Novidades</button>
        {categories.slice(0, 4).map((category) => <button key={category.id} onClick={() => { setActiveCategory(category.slug); go('home') }}>{category.nome}</button>)}
        <span className="nav-spacer" />
        <span className="nav-note"><Icon name="pin" size={17} /> Frete calculado no carrinho</span>
      </nav>
    </header>

    <main>
      {view === 'home' && <>
        <section className="hero container">
          <div className="hero-copy"><span className="hero-kicker"><span /> A VITRINE DO SEU JEITO</span><h1>Seu próximo achado está <em>por aqui.</em></h1><p>Explore produtos para a vida real, ideias novas e escolhas que fazem sentido para você.</p><button className="button button-dark hero-button" onClick={() => document.getElementById('catalogo')?.scrollIntoView({ behavior: 'smooth' })}>Explorar produtos <Icon name="arrow" size={19} /></button><div className="hero-small-note"><span className="note-line" /> Simples de encontrar. Bom de escolher.</div></div>
          <div className="hero-art" aria-hidden="true"><div className="art-circle art-circle-one"/><div className="art-circle art-circle-two"/><div className="art-panel"><span className="art-panel-top">cazuma<span>.</span></span><div className="art-orb"><div /></div><span className="art-panel-bottom">descubra o novo<br />todo dia <span>✳</span></span></div><div className="art-ticket">escolhas que<br /><strong>surpreendem ↗</strong></div><div className="art-star">✳</div></div>
        </section>
        <section className="value-strip container" aria-label="Como funciona"><div><span className="value-icon peach"><Icon name="search" size={23} /></span><span><strong>Explore sem pressa</strong><small>Encontre o que combina com você</small></span></div><div><span className="value-icon lavender"><Icon name="bag" size={23} /></span><span><strong>Escolha com clareza</strong><small>Preços e frete antes do pedido</small></span></div><div><span className="value-icon mint"><Icon name="spark" size={23} /></span><span><strong>De tudo um pouco</strong><small>Produtos físicos e digitais</small></span></div></section>
        <section className="catalog-section container" id="catalogo"><div className="section-heading"><div><span className="eyebrow">DESCUBRA NA CAZUMA</span><h2>Encontre seu próximo favorito<span>.</span></h2></div><p>Um universo de possibilidades em um só lugar.</p></div>
          <div className="catalog-toolbar"><div className="category-tabs" role="tablist" aria-label="Filtrar por categoria"><button className={activeCategory === 'todos' ? 'active' : ''} onClick={() => setActiveCategory('todos')}>Todos os produtos</button>{categories.map((category) => <button key={category.id} className={activeCategory === category.slug ? 'active' : ''} onClick={() => setActiveCategory(category.slug)}>{category.nome}</button>)}</div><div className="result-count"><Icon name="filter" size={18} /> {visibleProducts.length} {visibleProducts.length === 1 ? 'produto' : 'produtos'}</div></div>
          {loadError ? <div className="empty-state"><Icon name="package" size={42} /><h3>Não conseguimos carregar a vitrine.</h3><p>{loadError}</p><button className="button button-dark" onClick={loadCatalog}>Tentar novamente</button></div>
            : loading ? <div className="product-grid">{Array.from({ length: 4 }, (_, index) => <div className="product-skeleton" key={index} />)}</div>
              : visibleProducts.length ? <div className="product-grid">{visibleProducts.map((product) => <ProductCard key={product.id} product={product} onOpen={setSelected} onAdd={addProduct} />)}</div>
                : <div className="empty-state"><div className="empty-shape"><Icon name="package" size={42} /></div><h3>{search || activeCategory !== 'todos' ? 'Nenhum produto por aqui ainda.' : 'A vitrine está ficando pronta.'}</h3><p>{search || activeCategory !== 'todos' ? 'Tente outra busca ou explore todas as categorias.' : 'Os produtos aparecerão aqui assim que forem cadastrados.'}</p>{(search || activeCategory !== 'todos') && <button className="button button-outline" onClick={() => { setSearch(''); setActiveCategory('todos') }}>Ver todos os produtos</button>}</div>}
        </section>
        <section className="bottom-banner container"><span className="bottom-emblem">✳</span><div><span className="eyebrow">SEU CANTO DE DESCOBERTAS</span><h2>Tem sempre algo novo esperando por você.</h2></div><button className="button button-white" onClick={() => { setActiveCategory('todos'); setSearch(''); document.getElementById('catalogo')?.scrollIntoView({ behavior: 'smooth' }) }}>Ver catálogo <Icon name="arrow" size={18} /></button></section>
      </>}

      {view === 'cart' && <section className="inner-page container"><button className="text-back" onClick={() => go('home')}><Icon name="back" size={18} /> Continuar explorando</button><div className="page-heading"><span className="eyebrow">SUAS ESCOLHAS</span><h1>Meu carrinho<span>.</span></h1><p>Confira tudo antes de registrar seu pedido.</p></div>
        {!token ? <div className="empty-state"><div className="empty-shape"><Icon name="user" size={42} /></div><h3>Entre para ver seu carrinho.</h3><p>Suas escolhas ficam vinculadas à sua conta.</p><button className="button button-dark" onClick={() => requireAccount()}>Entrar ou criar conta</button></div>
          : !cart?.itens?.length ? <div className="empty-state"><div className="empty-shape"><Icon name="bag" size={42} /></div><h3>Seu carrinho está vazio.</h3><p>Que tal encontrar algo especial na vitrine?</p><button className="button button-dark" onClick={() => go('home')}>Explorar produtos</button></div>
            : <div className="cart-layout"><div className="cart-content"><div className="cart-items">{cart.itens.map((item) => { const product = products.find((entry) => entry.id === item.produtoId) || { nome: item.nome, tipo: item.tipo }; return <div className="cart-item" key={item.produtoId}><ProductVisual product={product} className="cart-visual" /><div className="cart-item-main"><span className="card-category">{item.tipo === 'DIGITAL' ? 'Produto digital' : 'Produto físico'}</span><strong>{item.nome}</strong><span>{money(item.precoUnitario)} cada</span><button className="remove-button" onClick={() => removeItem(item)} disabled={busy}><Icon name="trash" size={15} /> Remover</button></div><div className="cart-item-end"><strong>{money(item.subtotal)}</strong><div className="quantity-control"><button onClick={() => changeQuantity(item, item.quantidade - 1)} disabled={busy} aria-label={`Diminuir quantidade de ${item.nome}`}><Icon name="minus" size={16} /></button><span>{item.quantidade}</span><button onClick={() => changeQuantity(item, item.quantidade + 1)} disabled={busy} aria-label={`Aumentar quantidade de ${item.nome}`}><Icon name="plus" size={16} /></button></div></div></div> })}</div>
              {hasPhysical && <div className="checkout-card"><div className="checkout-title"><span className="checkout-icon"><Icon name="pin" /></span><div><h3>Para onde vamos enviar?</h3><p>Digite seu CEP para consultar o frete.</p></div></div><div className="cep-row"><input inputMode="numeric" maxLength={9} placeholder="00000-000" aria-label="CEP de entrega" value={cep} onChange={(e) => { setCep(e.target.value); setQuote(null) }} /><button className="button button-dark" onClick={calculateShipping} disabled={busy}>Calcular frete</button></div>{quote && <p className="quote-success"><Icon name="check" size={18} /> Frete para {quote.cep}: <strong>{money(quote.frete)}</strong></p>}</div>}
              <div className="checkout-card"><div className="checkout-title"><span className="checkout-icon"><Icon name="package" /></span><div><h3>Dados para o pedido</h3><p>{hasPhysical ? 'Preencha o endereço de entrega.' : 'Produto digital: não precisa de endereço.'}</p></div></div>{hasPhysical && <div className="address-grid">{[['logradouro', 'Rua / avenida'], ['numero', 'Número'], ['complemento', 'Complemento'], ['bairro', 'Bairro'], ['cidade', 'Cidade'], ['uf', 'UF']].map(([key, label]) => <label key={key} className={`address-${key}`}>{label}<input required={key !== 'complemento'} maxLength={key === 'uf' ? 2 : undefined} value={address[key]} onChange={(e) => setAddress({ ...address, [key]: e.target.value })} placeholder={label} /></label>)}</div>}</div></div>
              <aside className="order-summary"><h3>Resumo do pedido</h3><div><span>Produtos</span><strong>{money(cart.subtotal)}</strong></div><div><span>Frete</span><strong>{hasPhysical ? quote ? money(quote.frete) : 'Calcule com o CEP' : money(0)}</strong></div><div className="summary-total"><span>Total</span><strong>{hasPhysical && !quote ? 'Calcule o frete' : money(quote?.total ?? cart.subtotal)}</strong></div><p className="summary-note">Pagamento online ainda não está disponível. Registrar o pedido não faz cobrança.</p><button className="button button-dark full" disabled={busy || (hasPhysical && !quote)} onClick={placeOrder}>Registrar pedido <Icon name="arrow" size={19} /></button></aside></div>}
      </section>}

      {view === 'orders' && <section className="inner-page container"><button className="text-back" onClick={() => go('home')}><Icon name="back" size={18} /> Voltar à vitrine</button><div className="page-heading"><span className="eyebrow">SUA HISTÓRIA NA CAZUMA</span><h1>Meus pedidos<span>.</span></h1><p>Acompanhe os pedidos registrados na sua conta.</p></div>{orders.length ? <div className="orders-list">{orders.map((order) => <article className="order-card" key={order.id}><div className="order-card-head"><div><span className="eyebrow">PEDIDO #{order.id}</span><h3>{new Date(order.criadoEm).toLocaleDateString('pt-BR')}</h3></div><span className="status-pill">{order.status === 'AGUARDANDO_PAGAMENTO' ? 'Aguardando pagamento' : 'Aguardando frete'}</span></div><div className="order-names">{order.itens.map((item) => `${item.quantidade}× ${item.nome}`).join(' · ')}</div><div className="order-card-foot"><span>Frete: {money(order.frete)}</span><strong>Total: {money(order.total)}</strong></div></article>)}</div> : <div className="empty-state"><div className="empty-shape"><Icon name="package" size={42} /></div><h3>Nenhum pedido por enquanto.</h3><p>Quando você registrar um pedido, ele aparece aqui.</p><button className="button button-dark" onClick={() => go('home')}>Explorar produtos</button></div>}</section>}
    </main>

    <footer className="site-footer"><div className="container footer-content"><div><div className="footer-brand">cazuma<span>.</span></div><p>Boas escolhas moram aqui.</p></div><div className="footer-right"><span>Feito para descobrir coisas boas.</span><span>© {new Date().getFullYear()} Cazuma</span></div></div></footer>

    {selected && <Modal onClose={() => setSelected(null)} className="product-modal"><button className="icon-button modal-close" onClick={() => setSelected(null)} aria-label="Fechar"><Icon name="close" /></button><div className="product-detail"><ProductVisual product={selected} className="detail-visual" /><div className="detail-content"><span className="eyebrow">{selected.categoria?.nome || 'CAZUMA'} · {selected.tipo === 'DIGITAL' ? 'DIGITAL' : 'FÍSICO'}</span><h2>{selected.nome}</h2><p className="detail-description">{selected.descricao || 'Um novo achado para conhecer de perto.'}</p><strong className="detail-price">{money(selected.preco)}</strong><p className="detail-shipping">{selected.tipo === 'DIGITAL' ? 'Entrega digital após liberação do pedido.' : 'Calcule o frete no carrinho antes de registrar o pedido.'}</p>{selected.atributos && Object.keys(selected.atributos).length > 0 && <div className="detail-attributes">{Object.entries(selected.atributos).map(([key, value]) => <span key={key}><b>{key}:</b> {value}</span>)}</div>}<button className="button button-dark full" disabled={busy || (selected.tipo === 'FISICO' && selected.estoque < 1)} onClick={() => addProduct(selected)}>{selected.tipo === 'FISICO' && selected.estoque < 1 ? 'Indisponível' : 'Adicionar ao carrinho'} <Icon name="arrow" size={19} /></button></div></div></Modal>}
    {authOpen && <AuthModal mode={authMode} setMode={setAuthMode} onClose={() => { setAuthOpen(false); setPendingProduct(null) }} onSubmit={submitAuth} busy={busy} />}
    {toast && <div className="toast" role="status"><span><Icon name="spark" size={18} /></span>{toast}<button onClick={() => setToast('')} aria-label="Dispensar aviso"><Icon name="close" size={16} /></button></div>}
  </div>
}
