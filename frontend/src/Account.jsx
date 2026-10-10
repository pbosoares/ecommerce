import { useEffect, useState } from 'react'
import { request } from './api'

export const emptyAddress = { cep: '', logradouro: '', numero: '', complemento: '', bairro: '', cidade: '', uf: '' }
export function AddressFields({ address, onChange, includeCep = false }) {
  const fields = [...(includeCep ? [['cep', 'CEP']] : []), ['logradouro', 'Rua / avenida'], ['numero', 'Número'], ['complemento', 'Complemento'], ['bairro', 'Bairro'], ['cidade', 'Cidade'], ['uf', 'UF']]
  return <div className="address-grid">{fields.map(([key, label]) => <label key={key} className={`address-${key}`}>{label}<input required={key !== 'complemento'} maxLength={{ cep: 9, logradouro: 120, numero: 20, complemento: 120, bairro: 80, cidade: 80, uf: 2 }[key]} inputMode={key === 'cep' ? 'numeric' : undefined} pattern={key === 'cep' ? '[0-9]{5}-?[0-9]{3}' : key === 'uf' ? '[A-Z]{2}' : undefined} value={address[key] || ''} onChange={event => onChange({ ...address, [key]: key === 'uf' ? event.target.value.toUpperCase() : event.target.value })} /></label>)}</div>
}

export default function Account({ token, addresses, addressesLoading, addressesError, onAddressesChange, onShop, onOrders, onLogout, onLogin, notify }) {
  const [profile, setProfile] = useState(null)
  const [name, setName] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const [editing, setEditing] = useState(null)
  const [form, setForm] = useState({ apelido: '', endereco: emptyAddress })
  const [removeId, setRemoveId] = useState(null)
  async function load() {
    setLoading(true); setError('')
    try { const data = await request('/minha-conta', { token }); setProfile(data); setName(data.nome) }
    catch (failure) { setError(failure.message) }
    finally { setLoading(false) }
  }
  useEffect(() => { if (token) load() }, [token])
  async function perform(action) {
    setBusy(true); setError('')
    try { await action() } catch (failure) { setError(failure.message) } finally { setBusy(false) }
  }
  const edit = (saved) => { setEditing(saved?.id || 'new'); setForm(saved ? { apelido: saved.apelido, endereco: saved.endereco } : { apelido: '', endereco: { ...emptyAddress } }); setRemoveId(null) }
  return <section className="inner-page container account-page">
    <button className="text-back" onClick={onShop}>← Voltar à vitrine</button>
    <div className="page-heading"><span className="eyebrow">SEU ESPAÇO NA CAZUMA</span><h1>Minha conta<span>.</span></h1><p>Seus dados e endereços, prontos para a próxima compra.</p></div>
    {!token ? <div className="empty-state"><h3>Entre para acessar sua conta.</h3><button className="button button-dark" onClick={onLogin}>Entrar ou criar conta</button></div> : <>
      <div className="account-navigation"><button className="button button-outline" onClick={onOrders}>Meus pedidos e histórico</button><button className="button button-outline" onClick={onLogout}>Sair da conta</button></div>
      {error && <div className="account-error" role="alert"><p>{error}</p>{error.includes('sessão terminou') && <button className="button button-outline" onClick={onLogin}>Entrar novamente</button>}</div>}
      {loading ? <p role="status">Carregando seus dados…</p> : !profile ? <button className="button button-outline" onClick={load}>Tentar carregar conta novamente</button> : <div className="account-layout">
        <div className="checkout-card"><h2>Meus dados</h2><form className="auth-form" onSubmit={event => { event.preventDefault(); perform(async () => { const data = await request('/minha-conta', { token, method: 'PUT', body: JSON.stringify({ nome: name.trim() }) }); setProfile(data); setName(data.nome); notify('Nome atualizado.') }) }}><label>Seu nome<input required maxLength={80} autoComplete="name" value={name} onChange={event => setName(event.target.value)} /></label><label>E-mail<input value={profile.email} readOnly type="email" /></label><button className="button button-dark" disabled={busy || !name.trim() || name.trim() === profile.nome}>Salvar meus dados</button></form></div>
        <div className="checkout-card"><div className="account-address-heading"><div><h2>Endereços salvos</h2><p className="muted">Use seus endereços no carrinho, sem digitar tudo de novo.</p></div><button className="button button-dark" disabled={busy || editing !== null} onClick={() => edit(null)}>Novo endereço</button></div>
          {addressesLoading && <p role="status">Carregando endereços…</p>}
          {addressesError && <div className="account-error" role="alert"><p>{addressesError}</p><button className="button button-outline" onClick={onAddressesChange}>Tentar novamente</button></div>}
          {!addressesLoading && !addressesError && addresses.length === 0 && editing === null && <p className="address-empty">Você ainda não salvou um endereço. Cadastre sua casa, trabalho ou outro destino.</p>}
          <div className="saved-address-list">{addresses.map(saved => <article key={saved.id} className="saved-address"><strong>{saved.apelido}</strong><p>{saved.endereco.logradouro}, {saved.endereco.numero}{saved.endereco.complemento && ` · ${saved.endereco.complemento}`}<br />{saved.endereco.bairro} · {saved.endereco.cidade} / {saved.endereco.uf}<br />CEP {saved.endereco.cep}</p><div className="order-actions"><button className="button button-outline" disabled={busy || editing !== null} onClick={() => edit(saved)}>Editar</button><button className="button button-outline" disabled={busy} onClick={() => setRemoveId(saved.id)}>Excluir</button></div>{removeId === saved.id && <div className="address-delete" role="alert"><p>Excluir “{saved.apelido}” dos endereços salvos?</p><button className="button button-dark" disabled={busy} onClick={() => perform(async () => { await request(`/minha-conta/enderecos/${saved.id}`, { token, method: 'DELETE' }); setRemoveId(null); if (editing === saved.id) setEditing(null); await onAddressesChange(); notify('Endereço excluído.') })}>Confirmar exclusão</button><button className="button button-outline" disabled={busy} onClick={() => setRemoveId(null)}>Manter endereço</button></div>}</article>)}</div>
          {editing !== null && <form className="address-editor" onSubmit={event => { event.preventDefault(); perform(async () => { await request(`/minha-conta/enderecos${editing === 'new' ? '' : `/${editing}`}`, { token, method: editing === 'new' ? 'POST' : 'PUT', body: JSON.stringify({ apelido: form.apelido.trim(), endereco: { ...form.endereco, cep: form.endereco.cep.replace(/\D/g, ''), uf: form.endereco.uf.toUpperCase() } }) }); setEditing(null); await onAddressesChange(); notify('Endereço salvo na sua conta.') }) }}><h3>{editing === 'new' ? 'Novo endereço' : 'Editar endereço'}</h3><label className="address-nickname">Nome do endereço<input required maxLength={40} placeholder="Ex.: Casa ou Trabalho" value={form.apelido} onChange={event => setForm({ ...form, apelido: event.target.value })} /></label><AddressFields address={form.endereco} includeCep onChange={endereco => setForm({ ...form, endereco })} /><div className="order-actions"><button className="button button-dark" disabled={busy || !form.apelido.trim()}>{busy ? 'Salvando…' : 'Salvar endereço'}</button><button type="button" className="button button-outline" disabled={busy} onClick={() => setEditing(null)}>Cancelar</button></div></form>}
        </div>
      </div>}
    </>}
  </section>
}
