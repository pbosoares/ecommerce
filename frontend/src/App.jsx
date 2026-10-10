import { useEffect, useMemo, useState } from 'react'
import { downloadFile, money, request } from './api'
import Orders from './Orders'
import Account, { AddressFields, emptyAddress } from './Account'
import PasswordRecovery from './PasswordRecovery'


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
      <span className="card-caption">{product.tipo === 'DIGITAL' ? 'Acesso após pagamento' : `${product.estoque} em estoque · frete no carrinho`}</span>
      <button className="card-add" onClick={() => onAdd(product)} disabled={product.tipo !== 'DIGITAL' && product.estoque < 1}>
        <Icon name="plus" size={18} /> {product.tipo !== 'DIGITAL' && product.estoque < 1 ? 'Indisponível' : 'Adicionar'}
      </button>
    </div>
  </article>
}

function AuthModal({ mode, setMode, onClose, onSubmit, onForgot, busy }) {
  const [form, setForm] = useState({ nome: '', email: '', senha: '' })
  const signup = mode === 'register'
  return <Modal onClose={onClose} className="auth-modal">
    <button className="icon-button modal-close" onClick={onClose} aria-label="Fechar"><Icon name="close" /></button>
    <div className="auth-mark">c<span>.</span></div>
    <p className="eyebrow">Sua conta Cazuma</p>
    <h2>{signup ? 'Bom ter você por aqui.' : 'Que bom te ver de novo.'}</h2>
    <p className="muted">{signup ? 'Crie sua conta para guardar suas escolhas.' : 'Entre para continuar de onde parou.'}</p>
    <form onSubmit={(event) => { event.preventDefault(); onSubmit(form, signup) }} className="auth-form">
      {signup && <label>Seu nome<input required maxLength={80} autoComplete="name" value={form.nome} onChange={(e) => setForm({ ...form, nome: e.target.value })} placeholder="Como podemos te chamar?" /></label>}
      <label>E-mail<input required type="email" maxLength={254} autoComplete="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} placeholder="voce@exemplo.com" /></label>
      <label>Senha<input required type="password" minLength={8} maxLength={128} autoComplete={signup ? 'new-password' : 'current-password'} value={form.senha} onChange={(e) => setForm({ ...form, senha: e.target.value })} placeholder="Mínimo de 8 caracteres" /></label>
      <button className="button button-dark full" disabled={busy}>{busy ? 'Aguarde…' : signup ? 'Criar minha conta' : 'Entrar na minha conta'} <Icon name="arrow" size={18} /></button>
    </form>
    {!signup && <p className="auth-switch"><button onClick={onForgot} disabled={busy}>Esqueci minha senha</button></p>}
    <p className="auth-switch">{signup ? 'Já tem uma conta?' : 'Ainda não tem uma conta?'} <button onClick={() => setMode(signup ? 'login' : 'register')}>{signup ? 'Entrar' : 'Criar conta'}</button></p>
  </Modal>
}

export default function App() {
  const [products, setProducts] = useState([])
  const [categories, setCategories] = useState([])
  const [isDemo, setIsDemo] = useState(null)
  const [testPayments, setTestPayments] = useState(false)
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState('')
  const [search, setSearch] = useState('')
  const [activeCategory, setActiveCategory] = useState('todos')
  const [view, setView] = useState('home')
  const [catalogJump, setCatalogJump] = useState(0)
  const [selected, setSelected] = useState(null)
  const [token, setToken] = useState(() => sessionStorage.getItem('cazuma_token') || '')
  const [cart, setCart] = useState(null)
  const [orders, setOrders] = useState([])
  const [ordersLoading, setOrdersLoading] = useState(false)
  const [ordersError, setOrdersError] = useState('')
  const [pendingOrders, setPendingOrders] = useState(false)
  const [pendingAccount, setPendingAccount] = useState(false)
  const [savedAddresses, setSavedAddresses] = useState([])
  const [addressesLoading, setAddressesLoading] = useState(false)
  const [addressesError, setAddressesError] = useState('')
  const [savedAddressId, setSavedAddressId] = useState('')
  const [authOpen, setAuthOpen] = useState(false)
  const [recoveryToken, setRecoveryToken] = useState(() => new URLSearchParams(window.location.hash.slice(1)).get('redefinir-senha') || '')
  const [recoveryOpen, setRecoveryOpen] = useState(() => new URLSearchParams(window.location.hash.slice(1)).has('redefinir-senha'))
  const [authMode, setAuthMode] = useState('login')
  const [pendingProduct, setPendingProduct] = useState(null)
  const [busy, setBusy] = useState(false)
  const [toast, setToast] = useState('')
  const [cep, setCep] = useState('')
  const [quote, setQuote] = useState(null)
  const [orderKey, setOrderKey] = useState(() => sessionStorage.getItem('cazuma_order_key') || crypto.randomUUID())
  const [address, setAddress] = useState(emptyAddress)

  const notify = (message) => setToast(message)
  useEffect(() => {
    const readRecoveryLink = () => {
      const fragment = new URLSearchParams(window.location.hash.slice(1))
      if (fragment.has('redefinir-senha')) {
        setRecoveryToken(fragment.get('redefinir-senha') || ''); setRecoveryOpen(true); setAuthOpen(false)
        window.history.replaceState({}, '', window.location.pathname + window.location.search)
      }
    }
    readRecoveryLink()
    window.addEventListener('hashchange', readRecoveryLink)
    return () => window.removeEventListener('hashchange', readRecoveryLink)
  }, [])
  const closeRecovery = () => { setRecoveryOpen(false); setRecoveryToken('') }
  const recoveryLogin = () => {
    const changedPassword = Boolean(recoveryToken)
    closeRecovery(); setAuthMode('login'); setAuthOpen(true)
    if (changedPassword) { sessionStorage.removeItem('cazuma_token'); setToken(''); setCart(null); setOrders([]); setSavedAddresses([]) }
  }
  useEffect(() => { if (!toast) return; const id = setTimeout(() => setToast(''), 4500); return () => clearTimeout(id) }, [toast])

  const loadCatalog = async () => {
    setLoading(true); setLoadError('')
    try {
      const [items, groups, config] = await Promise.all([request('/produtos'), request('/categorias'), request('/loja/config')])
      setProducts(items); setCategories(groups); setIsDemo(config.demo)
      setTestPayments(config.pagamentoTeste === true)
    } catch (error) { setLoadError(error.message) }
    finally { setLoading(false) }
  }
  useEffect(() => { loadCatalog() }, [])
  useEffect(() => {
    let current = true
    if (token) request('/carrinho', { token }).then(data => { if (current) setCart(data) }).catch(() => {
      if (current) { sessionStorage.removeItem('cazuma_token'); setToken(''); setCart(null) }
    })
    return () => { current = false }
  }, [token])

  const visibleProducts = useMemo(() => products.filter((product) => {
    const inCategory = activeCategory === 'todos' || product.categoria?.slug === activeCategory
    const inSearch = `${product.nome} ${product.descricao || ''}`.toLocaleLowerCase('pt-BR').includes(search.trim().toLocaleLowerCase('pt-BR'))
    return inCategory && inSearch
  }), [products, activeCategory, search])
  const count = cart?.itens?.reduce((sum, item) => sum + item.quantidade, 0) || 0
  const hasPhysical = cart?.itens?.some((item) => item.tipo === 'FISICO')

  const go = (next) => { setView(next); setSelected(null); window.scrollTo({ top: 0, behavior: 'smooth' }) }
  const openCatalog = (category = 'todos') => {
    setActiveCategory(category); setSearch(''); setSelected(null); setView('home')
    setCatalogJump((current) => current + 1)
  }
  useEffect(() => {
    if (catalogJump > 0) document.getElementById('catalogo')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }, [catalogJump])
  const rotateOrderKey = () => { const key = crypto.randomUUID(); sessionStorage.setItem('cazuma_order_key', key); setOrderKey(key) }
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
      setCart(nextCart); setQuote(null); setSelected(null); rotateOrderKey(); notify(`${product.nome} foi para o carrinho.`)
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
    setCart(nextCart); setQuote(null); rotateOrderKey()
  })
  const removeItem = (item) => perform(async () => {
    await request(`/carrinho/itens/${item.produtoId}`, { token, method: 'DELETE' })
    setCart(await request('/carrinho', { token })); setQuote(null); rotateOrderKey()
  })
  const calculateShipping = () => perform(async () => {
    const clean = cep.replace(/\D/g, '')
    if (clean.length !== 8) throw new Error('Digite um CEP com 8 números.')
    const result = await request('/frete/cotacoes', { token, method: 'POST', body: JSON.stringify({ cep: clean }) })
    setQuote(result); setAddress((current) => ({ ...current, cep: clean }))
  })
  const placeOrder = () => perform(async () => {
    if (isDemo === null) throw new Error('Aguarde a configuração da loja carregar.')
    if (hasPhysical && !quote) throw new Error('Calcule o frete antes de registrar o pedido.')
    const body = hasPhysical ? { entrega: { ...address, cep: cep.replace(/\D/g, ''), uf: address.uf.toUpperCase() } } : {}
    const order = await request('/pedidos', { token, method: 'POST', headers: { 'Idempotency-Key': orderKey }, body: JSON.stringify(body) })
    rotateOrderKey()
    setCart(await request('/carrinho', { token })); setQuote(null); setCep(''); setAddress(emptyAddress)
    await loadCatalog()
    await loadOrders(); go('orders')
    if (isDemo) { notify(`Pedido #${order.id} de demonstração registrado. Nenhuma cobrança foi feita.`); return }
    try {
      const session = await request(`/pedidos/${order.id}/checkout`, { token, method: 'POST' })
      window.location.assign(session.url)
    } catch (error) { notify(`Pedido #${order.id} registrado. ${error.message}`) }
  })
  const startCheckout = (order) => perform(async () => {
    const session = await request(`/pedidos/${order.id}/checkout`, { token, method: 'POST' })
    window.location.assign(session.url)
  })
  const baixarDigital = (order, item) => perform(async () => {
    await downloadFile(`/pedidos/${order.id}/itens/${item.id}/download`, token, item.nome)
  })
  const loadAddresses = async () => {
    if (!token) return
    setAddressesLoading(true); setAddressesError('')
    try {
      const data = await request('/minha-conta/enderecos', { token })
      if (sessionStorage.getItem('cazuma_token') === token) setSavedAddresses(data)
    }
    catch (error) { if (sessionStorage.getItem('cazuma_token') === token) setAddressesError(error.message) }
    finally { if (sessionStorage.getItem('cazuma_token') === token) setAddressesLoading(false) }
  }
  const openAccount = () => {
    go('account')
    if (!token) { setPendingAccount(true); requireAccount() }
  }
  const logout = () => {
    sessionStorage.removeItem('cazuma_token'); setToken(''); setCart(null); setQuote(null); setCep(''); setAddress(emptyAddress); setSavedAddressId(''); setPendingAccount(false); setPendingOrders(false); go('home'); notify('Você saiu da sua conta.')
  }
  const chooseAddress = (id) => {
    setSavedAddressId(id); setQuote(null)
    const saved = savedAddresses.find(entry => String(entry.id) === id)
    const next = saved ? { ...saved.endereco } : { ...emptyAddress }
    setAddress(next); setCep(next.cep)
  }
  const loadOrders = async () => {
    go('orders')
    if (!token) { setPendingOrders(true); return requireAccount() }
    setOrdersLoading(true); setOrdersError('')
    try {
      const data = await request('/pedidos', { token })
      if (sessionStorage.getItem('cazuma_token') === token) setOrders(data)
    }
    catch (error) { if (sessionStorage.getItem('cazuma_token') === token) setOrdersError(error.message) }
    finally { if (sessionStorage.getItem('cazuma_token') === token) setOrdersLoading(false) }
  }
  useEffect(() => {
    setOrders([]); setOrdersError(''); setOrdersLoading(false); setSavedAddresses([]); setAddressesError(''); setAddressesLoading(false); setSavedAddressId('')
    if (token) loadAddresses()
    if (token && pendingAccount) { setPendingAccount(false); go('account') }
    if (token && pendingOrders) { setPendingOrders(false); loadOrders() }
  }, [token])

  useEffect(() => {
    const result = new URLSearchParams(window.location.search).get('checkout')
    if (!['success', 'cancelled'].includes(result)) return
    window.history.replaceState({}, '', window.location.pathname)
    if (token) loadOrders()
    notify(result === 'success'
      ? 'Pagamento enviado. Aguarde a confirmação para liberar o pedido.'
      : 'Pagamento não concluído. Você pode tentar novamente em Meus pedidos.')
  }, [])

  return <div className="site-shell">
    <div className="announcement"><span>Boas escolhas começam por aqui</span><span className="announcement-dot">✦</span><span>Produtos físicos e digitais em um só lugar</span></div>
    <header className="site-header">
      <div className="header-main container">
        <button className="brand" onClick={() => go('home')} aria-label="Cazuma, página inicial"><span className="brand-symbol">c<span className="brand-sun">●</span></span><span>cazuma<span className="brand-period">.</span></span></button>
        <label className="search-box"><Icon name="search" size={21} /><input maxLength={80} value={search} onChange={(event) => { setSearch(event.target.value); if (view !== 'home') go('home') }} placeholder="O que você está procurando?" aria-label="Buscar produtos" /><span className="search-shortcut">Buscar</span></label>
        <div className="header-actions">
          <button className="header-action account-action" aria-label={token ? 'Minha conta' : 'Entrar ou cadastrar'} onClick={openAccount}><Icon name="user" /><span>{token ? 'Minha conta' : 'Entrar / Cadastrar'}</span></button>
          <button className="header-action cart-action" aria-label={`Carrinho com ${count} itens`} onClick={() => go('cart')}><Icon name="bag" /><span>Carrinho</span>{count > 0 && <b className="cart-count">{count}</b>}</button>
        </div>
      </div>
      <nav className="header-nav container" aria-label="Navegação principal">
        <button onClick={() => openCatalog()}><Icon name="menu" size={18} /> Todas as categorias</button>
        <span className="nav-divider" />
        <button onClick={() => openCatalog()}>Novidades</button>
        {categories.slice(0, 4).map((category) => <button key={category.id} onClick={() => openCatalog(category.slug)}>{category.nome}</button>)}
        <button onClick={loadOrders}>Meus pedidos</button>
        <span className="nav-spacer" />
        <span className="nav-note"><Icon name="pin" size={17} /> Frete calculado no carrinho</span>
      </nav>
    </header>

    <main>
      {isDemo && <div className="demo-notice container" role="status"><strong>Loja de demonstração</strong><span>Produtos fictícios para testar a compra. Nenhuma cobrança será feita.</span></div>}
      {testPayments && <div className="demo-notice container" role="status"><strong>Compra de teste</strong><span>Produtos fictícios, sem cobrança ou entrega real. No pagamento, use o cartão de teste 4242 4242 4242 4242, uma validade futura e qualquer CVC de 3 dígitos.</span></div>}
      {view === 'home' && <>
        <section className="hero container">
          <div className="hero-copy"><span className="hero-kicker"><span /> A VITRINE DO SEU JEITO</span><h1>Seu próximo achado está <em>por aqui.</em></h1><p>Explore produtos para a vida real, ideias novas e escolhas que fazem sentido para você.</p><button className="button button-dark hero-button" onClick={() => openCatalog()}>Explorar produtos <Icon name="arrow" size={19} /></button><div className="hero-small-note"><span className="note-line" /> Simples de encontrar. Bom de escolher.</div></div>
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
        <section className="bottom-banner container"><span className="bottom-emblem">✳</span><div><span className="eyebrow">SEU CANTO DE DESCOBERTAS</span><h2>Tem sempre algo novo esperando por você.</h2></div><button className="button button-white" onClick={() => openCatalog()}>Ver catálogo <Icon name="arrow" size={18} /></button></section>
      </>}

      {view === 'cart' && <section className="inner-page container"><button className="text-back" onClick={() => go('home')}><Icon name="back" size={18} /> Continuar explorando</button><div className="page-heading"><span className="eyebrow">SUAS ESCOLHAS</span><h1>Meu carrinho<span>.</span></h1><p>Confira tudo antes de registrar seu pedido.</p></div>
        {!token ? <div className="empty-state"><div className="empty-shape"><Icon name="user" size={42} /></div><h3>Entre para ver seu carrinho.</h3><p>Suas escolhas ficam vinculadas à sua conta.</p><button className="button button-dark" onClick={() => requireAccount()}>Entrar ou criar conta</button></div>
          : !cart?.itens?.length ? <div className="empty-state"><div className="empty-shape"><Icon name="bag" size={42} /></div><h3>Seu carrinho está vazio.</h3><p>Que tal encontrar algo especial na vitrine?</p><button className="button button-dark" onClick={() => openCatalog()}>Explorar produtos</button></div>
            : <div className="cart-layout"><div className="cart-content"><div className="cart-items">{cart.itens.map((item) => { const product = products.find((entry) => entry.id === item.produtoId) || { nome: item.nome, tipo: item.tipo }; return <div className="cart-item" key={item.produtoId}><ProductVisual product={product} className="cart-visual" /><div className="cart-item-main"><span className="card-category">{item.tipo === 'DIGITAL' ? 'Produto digital' : 'Produto físico'}</span><strong>{item.nome}</strong><span>{money(item.precoUnitario)} cada</span><button className="remove-button" onClick={() => removeItem(item)} disabled={busy}><Icon name="trash" size={15} /> Remover</button></div><div className="cart-item-end"><strong>{money(item.subtotal)}</strong><div className="quantity-control"><button onClick={() => changeQuantity(item, item.quantidade - 1)} disabled={busy} aria-label={`Diminuir quantidade de ${item.nome}`}><Icon name="minus" size={16} /></button><span>{item.quantidade}</span><button onClick={() => changeQuantity(item, item.quantidade + 1)} disabled={busy || (item.tipo === 'FISICO' && item.quantidade >= product.estoque)} aria-label={`Aumentar quantidade de ${item.nome}`}><Icon name="plus" size={16} /></button></div></div></div> })}</div>
              {hasPhysical && <div className="checkout-card"><div className="checkout-title"><span className="checkout-icon"><Icon name="pin" /></span><div><h3>Para onde vamos enviar?</h3><p>Digite seu CEP para consultar o frete.</p></div></div><div className="checkout-saved-addresses"><label>Usar um endereço salvo<select value={savedAddressId} onChange={event => chooseAddress(event.target.value)} disabled={addressesLoading}><option value="">Digitar outro endereço</option>{savedAddresses.map(saved => <option key={saved.id} value={saved.id}>{saved.apelido} — {saved.endereco.cidade} / {saved.endereco.uf}</option>)}</select></label><button className="text-back" onClick={openAccount}>Gerenciar endereços</button>{addressesLoading && <p role="status">Carregando endereços…</p>}{addressesError && <p role="alert">{addressesError} <button onClick={loadAddresses}>Tentar novamente</button></p>}</div><div className="cep-row"><input inputMode="numeric" maxLength={9} placeholder="00000-000" aria-label="CEP de entrega" value={cep} onChange={(e) => { setCep(e.target.value); setAddress(current => ({ ...current, cep: e.target.value.replace(/\D/g, '') })); setSavedAddressId(''); setQuote(null) }} /><button className="button button-dark" onClick={calculateShipping} disabled={busy}>Calcular frete</button></div>{quote && <p className="quote-success"><Icon name="check" size={18} /> Frete para {quote.cep}: <strong>{money(quote.frete)}</strong></p>}</div>}
              <div className="checkout-card"><div className="checkout-title"><span className="checkout-icon"><Icon name="package" /></span><div><h3>Dados para o pedido</h3><p>{hasPhysical ? 'Preencha o endereço de entrega.' : 'Produto digital: não precisa de endereço.'}</p></div></div>{hasPhysical && <AddressFields address={address} onChange={next => { setAddress(next); setSavedAddressId('') }} />}</div></div>
              <aside className="order-summary"><h3>Resumo do pedido</h3><div><span>Produtos</span><strong>{money(cart.subtotal)}</strong></div><div><span>Frete</span><strong>{hasPhysical ? quote ? money(quote.frete) : 'Calcule com o CEP' : money(0)}</strong></div><div className="summary-total"><span>Total</span><strong>{hasPhysical && !quote ? 'Calcule o frete' : money(quote?.total ?? cart.subtotal)}</strong></div><p className="summary-note">{isDemo === null ? 'Carregando configuração da loja…' : isDemo ? 'Demonstração: registrar o pedido não faz cobrança.' : 'Você será direcionado ao pagamento seguro após registrar o pedido.'}</p><button className="button button-dark full" disabled={busy || isDemo === null || (hasPhysical && !quote)} onClick={placeOrder}>{isDemo ? 'Simular pedido' : 'Ir para pagamento'} <Icon name="arrow" size={19} /></button></aside></div>}
      </section>}

      {view === 'account' && <Account token={token} addresses={savedAddresses} addressesLoading={addressesLoading} addressesError={addressesError} onAddressesChange={loadAddresses} onShop={() => go('home')} onOrders={loadOrders} onLogout={logout} onLogin={() => { setPendingAccount(true); requireAccount() }} notify={notify} />}

      {view === 'orders' && <Orders orders={orders} loading={ordersLoading} error={ordersError} onRefresh={loadOrders} onShop={() => go('home')} onPay={startCheckout} onDownload={baixarDigital} busy={busy} isDemo={isDemo} authenticated={Boolean(token)} onLogin={() => { setPendingOrders(true); requireAccount() }} />}

    </main>

    <footer className="site-footer"><div className="container footer-content"><div><div className="footer-brand">cazuma<span>.</span></div><p>Boas escolhas moram aqui.</p></div><div className="footer-right"><span>Feito para descobrir coisas boas.</span><span>© {new Date().getFullYear()} Cazuma</span></div></div></footer>

    {selected && <Modal onClose={() => setSelected(null)} className="product-modal"><button className="icon-button modal-close" onClick={() => setSelected(null)} aria-label="Fechar"><Icon name="close" /></button><div className="product-detail"><ProductVisual product={selected} className="detail-visual" /><div className="detail-content"><span className="eyebrow">{selected.categoria?.nome || 'CAZUMA'} · {selected.tipo === 'DIGITAL' ? 'DIGITAL' : 'FÍSICO'}</span><h2>{selected.nome}</h2><p className="detail-description">{selected.descricao || 'Um novo achado para conhecer de perto.'}</p><strong className="detail-price">{money(selected.preco)}</strong><p className="detail-shipping">{selected.tipo === 'DIGITAL' ? 'Entrega digital após liberação do pedido.' : 'Calcule o frete no carrinho antes de registrar o pedido.'}</p>{selected.atributos && Object.keys(selected.atributos).length > 0 && <div className="detail-attributes">{Object.entries(selected.atributos).map(([key, value]) => <span key={key}><b>{key}:</b> {value}</span>)}</div>}<button className="button button-dark full" disabled={busy || (selected.tipo === 'FISICO' && selected.estoque < 1)} onClick={() => addProduct(selected)}>{selected.tipo === 'FISICO' && selected.estoque < 1 ? 'Indisponível' : 'Adicionar ao carrinho'} <Icon name="arrow" size={19} /></button></div></div></Modal>}
    {authOpen && <AuthModal mode={authMode} setMode={setAuthMode} onClose={() => { setAuthOpen(false); setPendingProduct(null) }} onSubmit={submitAuth} onForgot={() => { setAuthOpen(false); setRecoveryToken(''); setRecoveryOpen(true) }} busy={busy} />}
    {recoveryOpen && <PasswordRecovery key={recoveryToken || 'request'} Modal={Modal} resetToken={recoveryToken} onClose={closeRecovery} onLogin={recoveryLogin} onRequestNew={() => setRecoveryToken('')} onReset={() => { sessionStorage.removeItem('cazuma_token'); setToken(''); setCart(null); setOrders([]); setSavedAddresses([]); setQuote(null); setCep(''); setAddress(emptyAddress) }} />}
    {toast && <div className="toast" role="status"><span><Icon name="spark" size={18} /></span>{toast}<button onClick={() => setToast('')} aria-label="Dispensar aviso"><Icon name="close" size={16} /></button></div>}
  </div>
}
